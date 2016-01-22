package org.apache.scala.topic_discover

import java.util.regex.Pattern
import java.util.{Date, Properties}
import java.text.SimpleDateFormat

import org.apache.java.entity.{topic, ClusterPair, Cluster}
import org.apache.java.jdbc.MySqlData
import org.apache.spark.storage.StorageLevel

import scala.collection.JavaConversions._
import scala.collection.immutable.Map
import scala.collection.immutable.List
import scala.collection.mutable.{ArrayBuffer, LinkedHashMap}

import org.apache.spark.sql.functions.max
import org.apache.spark.sql.{SaveMode, SQLContext}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd._

import org.ansj.domain.Term
import org.ansj.splitWord.analysis.ToAnalysis

/**
 * Created by david on 11/13/15.
 */

case class p_b(id: Long
               , post_id: String
               , topic_id: Int, similarity: Double)

object TopicDiscover {
  //  val format = new SimpleDateFormat("hh:mm:ss.SSS")
  def generate_aw_kwl_fd(extract_data: RDD[(String, String)], stop_words: Array[String])
  : (RDD[(String, String)], RDD[(String, Int)], RDD[((String, String), Double)]) = {
    val key_thresold = 50

    val raw_wordList: RDD[(String, java.util.List[Term])] = extract_data.map {
      case (id, (content)) =>
        (id, ToAnalysis.parse(content))
    }

    val filter_wordList = raw_wordList.map {
      case (id, content) => (id, content.filter {
        case term =>
          val tname = term.getName()
          tname.size > 1 && (!stop_words.contains(tname))
      })
    }
    val filterd_wordList = filter_wordList.flatMap[(String, String)] {
      case (id, content) =>
        val buffer = {
          for (term <- content
               if ((term.getNatureStr().contains("n"))
                 || (term.getNatureStr().contains("userDefine"))))
            yield (id, term.getName())
        }
        buffer.iterator.asInstanceOf[TraversableOnce[(String, String)]]
    }
    val keywordList_ids = filterd_wordList.map { case e => (e._2, e._1) }
      .groupByKey().filter {
      _._2.size > key_thresold
    }

    val keywordList = keywordList_ids.map { e => (e._1, e._2.size) }.cache

    val combAndCount = keywordList_ids.cartesian(keywordList_ids)
      .coalesce(24)
      .filter { case (a, b) => a._1 < b._1 }.flatMap {
      case (a, b) =>
        var comb: Int = 0
        a._2.map { e => comb += b._2.count(_ == e) }
        Iterable(((a._1, b._1), comb, a._2.size, b._2.size))
    }

    val order = Ordering.by[((String, String), Double), Double](_._2)
    val allDistance = combAndCount
      .filter(_._2 != 0)
      .map { case (kw_pair, c, ap1, ap2) =>
      (kw_pair, (0.5 * c.toDouble) / ap1 + (0.5 * c.toDouble) / ap2)
    }
    val minDistance = allDistance.min()(order)._2
    val finalDistance = combAndCount
      .filter(_._2 == 0)
      .map { case (kw_pair, c, ap1, ap2) => (kw_pair, minDistance) }
      .union(allDistance)
      .map { case (kw_pair, dist) => (kw_pair, 1 / dist) }
      .cache
    (filterd_wordList, keywordList, finalDistance)
  }

  //merge sorted_DFD in the dateFD and changed the dateKWL
  def merge(sorted_DFD: ClusterPair
            , initKeywordList: ArrayBuffer[Cluster]
            , distance_matrix: ArrayBuffer[ClusterPair]): Unit = {

    initKeywordList -= sorted_DFD.getlCluster() -= sorted_DFD.getrCluster()
    distance_matrix -= sorted_DFD
    val newdateFD = distance_matrix.filter {
      case clusterpair =>
        val right = clusterpair.getrCluster()
        val left = clusterpair.getlCluster()
        val s_right = sorted_DFD.getrCluster()
        val s_left = sorted_DFD.getlCluster()
        if (right != s_right && right != s_left && left != s_right && left != s_left)
          false
        else
          true
    }
    val newclass = sorted_DFD.agglomerate(null)
    //.groupBy(_._1).map { case (a, b) => (a, b.map(_._2).reduce(_ + _) / b.size) }
    //.filter { case ((a, b), distance) => a.toString < b.toString } ++ (remainFD)
    var dis1: Double = 0.0
    var dis2: Double = 0.0
    for (keyword <- initKeywordList) {
      if (keyword.compareTo(sorted_DFD.getlCluster()) < 0) {
        dis1 = newdateFD.filter { e => (e.getlCluster() == keyword && e.getrCluster() == sorted_DFD.getlCluster()) }(0).getLinkageDistance
      } else {
        dis1 = newdateFD.filter { e => (e.getlCluster() == sorted_DFD.getlCluster() && e.getrCluster() == keyword) }(0).getLinkageDistance
      }
      if (keyword.compareTo(sorted_DFD.getrCluster()) < 0) {
        dis2 = newdateFD.filter { e => (e.getlCluster() == keyword && e.getrCluster() == sorted_DFD.getrCluster()) }(0).getLinkageDistance
      } else {
        dis2 = newdateFD.filter { e => (e.getlCluster() == sorted_DFD.getrCluster() && e.getrCluster() == keyword) }(0).getLinkageDistance
      }
      if (keyword.compareTo(newclass) < 0)
        distance_matrix.append(new ClusterPair(keyword, newclass, (dis1 + dis2) / 2))
      else
        distance_matrix.append(new ClusterPair(newclass, keyword, (dis1 + dis2) / 2))
    }
    distance_matrix --= newdateFD
    initKeywordList += newclass
  }

  def calculateQuality(classes: ArrayBuffer[Cluster]): Double = {
    var dis_sum: Double = 0.0
    val recent_class = classes.last.getDistance
    if (recent_class != null) {
      dis_sum = recent_class
    } else {
      dis_sum = classes.size
    }
    dis_sum / classes.size
  }

  def getInitClasses(clusters: ArrayBuffer[Cluster], distance_matrix
  : ArrayBuffer[ClusterPair]): LinkedHashMap[Double, ArrayBuffer[Cluster]] = {

    var decision = LinkedHashMap[Double, ArrayBuffer[Cluster]]()

    var sorted_DFD: Option[ClusterPair] = null

    def find_min(kv1: ClusterPair, kv2: ClusterPair) = {
      val r = kv1.compareTo(kv2)
      if (r <= 0) kv1 else kv2
    }
    if (distance_matrix.size > 0)
      sorted_DFD = Option(distance_matrix.reduce(find_min))
    else
      sorted_DFD = Option.empty
    //Print For Debug------------------
    //    println("\r"+format.format(new Date())+"\tfirst loop,min distance pair is :"+sorted_DFD)
    //---------------------------------
    //add another condition that dateFD.count!=0
    // as the bug that if the words is all into one class,the loop will not stop properly
    while (!sorted_DFD.isEmpty) {
      merge(sorted_DFD.get, clusters, distance_matrix)
      decision.put(calculateQuality(clusters), clusters.clone())
      if (distance_matrix.size > 0)
        sorted_DFD = Option(distance_matrix.reduce(find_min))
      else
        sorted_DFD = Option.empty
    }
    decision
  }

  def getDocTopicSimilaritycal(newTopic: List[(Int, String)], wordlist: List[String], count_sum: Int): Double = {
    var simi: Double = 0.0
    for (s1 <- wordlist) {
      for (s2 <- newTopic) {
        if (s1.equals(s2._2))
          simi = simi + s2._1.toDouble / count_sum
      }
    }
    simi
  }

  def getMaxRelatedSimiDocTopic(topics: List[(Int, topic)], id_words: RDD[(String, scala.Iterable[String])]
                                , count_sum: Int): (RDD[(Int, (Iterable[String], String))], RDD[(String, (Int, Double))]) = {
    val simi = id_words.flatMap {
      case (pid, wordlists) =>
        topics.map { case (tid, topic) =>
          //          println("\r"+format.format(new Date())+"\t"+topic.topic_keywords)
          (tid, pid, getDocTopicSimilaritycal(topic.topic_keywords, wordlists.toList, count_sum))
        }
    }.filter {
      _._3 != 0
    }
    //if a topic is supported less than 4 posts, it will be removed
    val process_simi = simi.map { case (tid, pid, simi) => (pid, (tid, simi)) }.groupByKey.map {
      case (pid, simis) =>
        val tmp_simi = simis.maxBy(_._2)
        (tmp_simi._1, (pid, tmp_simi._2))
    }.groupByKey.filter(_._2.size > 3)

    val max_topicid = process_simi.flatMap { case (tid, ite) => ite.map { e => (e._1, (tid, e._2)) } }

    //the most relative post and related_post list
    val max_postid_pro = process_simi.map {
      case (tid, simis) =>
        (tid, (simis.map(_._1), simis.maxBy(_._2)._1))
    }
    //.reduce{(e1,e2)=>if(e1._2<e2._2) e2 else e1}._1\
    (max_postid_pro, max_topicid)
  }

  // Input:
  // 1.date_filteredClasses: Map[String, ArrayBuffer[LinkedList[String]]]  (date,Array(class))
  // 2.extract_data: RDD[(String, (Timestamp, String))]              (post_id,(post_time,content))  in disk
  def turnTopicsAndOutput(clusters: ArrayBuffer[Cluster],
                          allwords: RDD[(String, String)],
                          extract_data: RDD[(String, String)],
                          keywordList: RDD[(String, Int)],
                          sc: SparkContext,
                          sqlContext: SQLContext,
                          hostname: String,
                          dbname: String,
                          dbuser: String,
                          dbpwd: String): Unit = {
    val count_sum = keywordList.reduce { case (a, b) => (a._1, a._2 + b._2) }._2
    val desWithCount = sc.parallelize(
      clusters.zipWithIndex.toSeq.flatMap { case (cluster, cid) => cluster.getName.split("&").map { case word => (word, cid) }
      }).join(keywordList)
      .map { case (word, (cid, count)) => (cid, (count, word)) }.groupByKey().map {
      case (cid, iter) =>
        (cid, iter.toList.sortBy { x => x._1 }(Ordering[Int].reverse))
    }
    val topics = desWithCount.collect.sortBy(_._1).map { case (cid, list) => topic(list) }.map { e => (e.topic_id, e) }
    val idwords = allwords.groupByKey()
    //Print For Debug------------------
    //    println("\r"+format.format(new Date())+"\tidwords :"+idwords.count)
    //    println("\r"+format.format(new Date())+"\ttopics :"+topics.size)
    //    println("\r"+format.format(new Date())+"\tcount_sum: "+count_sum)
    //---------------------------------
    val (maxpid, maxtid) = getMaxRelatedSimiDocTopic(topics.toList, idwords, count_sum)
    val compeleted_topic = sc.parallelize(topics).join(maxpid).map { case (tid, (topic, (ite, pid))) => (pid, (tid, topic, ite)) }.join(extract_data)
      .map { case (doc_id, ((tid, topic, ite), content)) =>
      val pattern = Pattern.compile("[https]{4,5}\\:\\/\\/([a-zA-Z]|[0-9])*((\\.|-)([a-zA-Z]|[0-9])*)*((\\/|-)([a-zA-Z]|[0-9])*)*((\\.|-)[a-zA-Z]*)*\\s?|\\u0022+|\\u0027+|([a-zA-Z]|[0-9])*((\\.|-)([a-zA-Z]|[0-9])*)*[\\.com]{4}|[…]{1,}\\s?|#+|@+|((\\/|-)([a-zA-Z]|[0-9])*)*")
      val matcher = pattern.matcher(content)
        .replaceAll("")
        .replaceAll("\\(转载\\)", "")
        .replaceAll("\\\"\' =<>", "")
      topic.summary = content
      topic.doc_related = ite.toList
      (tid, topic)
    }
    //Print For Debug------------------
    //    println("\r"+format.format(new Date())+"\twe will produce "+maxtid.count+" similarity pairs into post_topic")
    //    println("\r"+format.format(new Date())+"\tand will produce "+compeleted_topic.count+" topics into table topic")
    //---------------------------------
    val mySqlData = new MySqlData(hostname, dbname, dbuser, dbpwd)

    import sqlContext.implicits._
    val df_post_bursttopic = maxtid.zipWithIndex().map { case (p, index) => p_b(index, p._1, p._2._1, p._2._2) }.toDF()
    val property = new Properties()
    val url = "jdbc:mysql://" + hostname + ":3306/" + dbname
    property.put("user", dbuser)
    property.put("password", dbpwd)
    df_post_bursttopic.write.mode(SaveMode.Append).jdbc(url, "post_topic", property)

    compeleted_topic.sortByKey().map(_._2).collect.foreach(e => e.intoDB(mySqlData))
  }

  def findBestDecision(decision: LinkedHashMap[Double, ArrayBuffer[Cluster]]): ArrayBuffer[Cluster] = {
    val ordered_avedis = decision.keySet.toBuffer[Double].sortBy(e => e)(Ordering[Double].reverse)
    val scale = {
      for (i <- 2 until ordered_avedis.size - 2)
        yield ordered_avedis(i) / ordered_avedis(i + 1)
    }
    val maxindex = scale.zipWithIndex.maxBy(e => e._1)._2 + 2
    val maxavedis = ordered_avedis(maxindex)
    //Print For Debug------------------
    //    println("\r"+format.format(new Date())+"\tmax_AveDistance is :"+maxavedis)
    //---------------------------------
    decision.get(maxavedis).get
  }

  def do_TopicDiscover(hdfsPrefix: String, hostname: String, dbname: String, dbuser: String, dbpwd: String, sc: SparkContext, sqlContext: SQLContext) {
    //Print For Debug-----------
    //    println(format.format(new Date())+"\tStarting!")
    //--------------------------
    //		val sparkConf = new SparkConf().setAppName("TopicDiscover") // An existing SparkContext.
    //		val sc = new SparkContext(sparkConf)
    //		val hostname = "192.168.199.35"
    //		    val hdfshost="namenode1"
    //		val hdfshost = "ubuntu"
    val url = "jdbc:mysql://" + hostname + ":3306/" + dbname + "?user=" + dbuser + "&password=" + dbpwd
    //		val sqlContext = new SQLContext(sc)
    //    import sqlContext.implicits._
    //    driver
    //    partitionColumn, lowerBound, upperBound, numPartitions//Print For Debug!--------
    val jdbcDF = sqlContext.read.format("jdbc").options(
      Map("url" -> url,
        "dbtable" -> "post")).load() //&password=root
    val burst_topic_maxid = sqlContext.read.format("jdbc").options(
        Map("url" -> url,
          "dbtable" -> "topic")).load()
    val topic_maxid = burst_topic_maxid.agg(max("topic_id")).head.getInt(0)
    topic.last_topic_id_(topic_maxid)
    //Print For Debug------------------
    //    println(format.format(new Date())+"\tthe max topic id is :"+topic_maxid)
    //---------------------------------
    val extract_data = jdbcDF.filter("language_type=0 and data_type=2 and choice=1").map {
      row =>
        (row.getString(row.fieldIndex("id")),
          row.getString(row.fieldIndex("content"))
            .replaceAll("[0-9a-zA-Z_/\\:!@#$%^&*()]", "")
            .replace("\r", "")
            .replace("\n", "")
            .replace(" ", "")
            .replace("\t", "")
          )
    }.repartition(24).persist(StorageLevel.MEMORY_AND_DISK)
    val stop_words = sc.textFile(hdfsPrefix + "stop_words_td.txt").collect()
    //Print For Debug-----------
    //    println(format.format(new Date())+"\tthe count of raw data is:"+extract_data.count)
    //--------------------------

    val (allwords, keywordList, finalDistance) =
      generate_aw_kwl_fd(extract_data, stop_words)
    //Print For Debug-----------
    //    println("\r"+format.format(new Date())+"\tget all normal words,all keyword's list and this one-one distance pair")
    //    println("\r"+format.format(new Date())+"\tthe keyword num is :"+keywordList.count)
    //    println("\r"+format.format(new Date())+"\tthe distance pair num is :"+finalDistance.count)
    //--------------------------
    val Word2Cluster = keywordList.map { e => (e._1, new Cluster(e._1)) }.collect().toMap
    var dateKWL = ArrayBuffer[Cluster]() ++ Word2Cluster.values
    //.filter {_._3 < 2 * thresold}
    var dateFD = ArrayBuffer[ClusterPair]() ++=
      finalDistance.map { e =>
        new ClusterPair(Word2Cluster(e._1._1), Word2Cluster(e._1._2), e._2)
      }.collect

    val decision: LinkedHashMap[Double, ArrayBuffer[Cluster]] = getInitClasses(dateKWL, dateFD)
    val classes: ArrayBuffer[Cluster] = findBestDecision(decision)
    //Print For Debug------------------
    //    println("\r"+format.format(new Date())+"\tthe best decision is :"+decision)
    //    println("\r"+format.format(new Date())+"\tthe classes num is :"+classes.size)
    //    println("\r"+format.format(new Date())+"\tprecisely :")
    //    classes.zipWithIndex.foreach{e=>println("\t\t\t\t"+e._2+"\t"+e._1)}
    //---------------------------------
    turnTopicsAndOutput(classes, allwords, extract_data, keywordList, sc, sqlContext, hostname, dbname, dbuser, dbpwd)
    //Print For Debug------------------
    //    println("\r"+format.format(new Date())+"\tStoping!")
    //---------------------------------
    //		sc.stop()
  }
}












