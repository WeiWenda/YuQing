package org.apache.scala.relation_detect
/**
 * Created by luan on 15-11-20.
 */
import java.util.Properties
import de.bwaldvogel.liblinear.Predict
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.scala.Relation_build.Relation
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.spark.sql.{SQLContext, Row, SaveMode}
import org.apache.spark.{SparkConf, SparkContext}
import scala.collection.immutable.HashMap
import scala.collection.mutable.{ArrayBuffer, HashSet}

object Relation_Detector extends java.io.Serializable {
  /**
  @author Luan Yingying
  @return none
  @version 1.0.0
    */

  /**
   * hdfs:///home/skyclass/Data_Relation/sempair
   * hdfs:///home/skyclass/Data_Relation/binsen
   * hdfs:///home/skyclass/Data_Relation/CVMC.dat
   * hdfs:///home/skyclass/Data_Relation/Model.model
   */

  /**
   * relation_luan      line    126
   * entitymention_98   line    58
   * post_98    line.84
   */

  def do_relationDetect(Prefix_path:String,dburl:String, dbuser:String,dbpwd:String,sc:SparkContext,sqlContext:SQLContext) = {
    val conf = new SparkConf().setAppName("Relation_detector")
//    val sc = new SparkContext(conf)
//    val sqlContext = new org.apache.spark.sql.SQLContext(sc)
//    lazy val url = "jdbc:mysql://192.168.199.204:3306/yuqing"
    lazy val  url=dburl
//    lazy val user = "xiaopang"
//    lazy val password = "123456"
    lazy val user = dbuser
    lazy val password = dbpwd
    lazy val entitymention = "entitymention"
    lazy val post = "post"
    lazy val relation = "relation"
    lazy val relationmention = "relationmention"
    val input_Hbase_w = Prefix_path+"Data_Relation/input_Hbase"
    val sempairList = sc.textFile(Prefix_path+"Data_Relation/sempair").cache().collect()
    val binsen = sc.textFile( Prefix_path+"Data_Relation/binsen").cache().collect()
    val dic_cvmc = sc.textFile( Prefix_path+"Data_Relation/CVMC.dat").cache()
    val Model =  Prefix_path+"Data_Relation/Model.model"
    val Out =  Prefix_path+"Data_Relation/Out"
    val input_Hbase_r = Prefix_path+"Data_Relation/input_Hbase/part-00000"

    val jdbcDF = sqlContext.read.format("jdbc").options(
      Map("url" -> url, "user" -> user, "password" -> password,
        "dbtable" -> entitymention)).load().repartition(24)

    //get results of table entitymention_98

    val entitymentions = jdbcDF.map { x => new EntityMention(x(0).toString, x(1).toString,
      x(2).toString, x(3).toString, x(4).toString, x(6).toString, x(9).toString, x(10).toString,
      x(11).toString, x(13).toString, x(14).toString)
    }.cache()
    //println("The count of data of table entitymention_98 is " + entitymentions.count())

    //get dictionary

    val dic_map = dic_cvmc.map(line => line.split(",")).collect()
    val dic_buffer = new ArrayBuffer[String]
    dic_map.foreach(i => i.foreach(a => (dic_buffer += a)))

    //build feature space

    //build the initial matrix

    var matrix = new ArrayBuffer[Array[Int]]()

    //get the entitymention of the same HbaseID

    val sameHbaseIDRDD = entitymentions.groupBy(x => x.HbaseID).cache()
    val sameHbaseIDs = sameHbaseIDRDD.collect().toMap
    //    val HbaseIDListRDD = sameHbaseIDRDD.map{case(k,v) => k}
    val HbaseIDListRDD = sameHbaseIDRDD.keys
    //get documentMap

    val documentMap = sqlContext.read.format("jdbc").options(
      Map("url" -> url, "user" -> user, "password" -> password,
        "dbtable" -> post)).load().repartition(24).
      map(x => (x(0).toString , Array(x(5).toString, x(6).toString))).cache()

    //post_topic_map<HbaseID,[topicIDs of Entitymention of this HbaseID]>

    val textList = documentMap.collect().toMap

    println("There are " + HbaseIDListRDD.count() + " texts(HbaseID) in total")
    var countOfDealt = 0
    var candidateList = new ArrayBuffer[RelationMention]()
    var relation_mentionList = new ArrayBuffer[RelationMention]()
    var relationListRDD = sc.parallelize(new ArrayBuffer[Relation]())
    var key = 1

    //-------------------------------------------------------
    //    initialmatrix("20150401114742-583113482918240256-00000000000www.twitter.com")
    lazy val h = documentMap.flatMap [(RelationMention, Array[Int])]{ case (k, v) => initialmatrix(k) }
    lazy val R_m = h.zipWithUniqueId().collect()
    candidateList ++= R_m.map{case (k,v) => k._1}
    matrix ++= R_m.map{case (k,v) => k._2}

    //add total feature to the vector

    modifyMatrix()
    predicInput()
    lazy val relationList = generateRelation()
    println("relationList:"+relationList.size)


    lazy val property = new Properties()
    property.put("user", user)
    property.put("password", password)
    lazy val remen_rdd = sc.parallelize(relation_mentionList).map(item => Row.apply(item.id, item.relationType,
      item.entitymention1ID, item.entity1Name, item.entitymention2ID, item.entity2Name, item.HbaseID,
      item.topicID.toString, item.postTime, item.insertTime))
    lazy val re_rdd =sc.parallelize(relationList).map(item => Row.apply(item.relationID,item.relationType,
      item.entity1ID,item.entity1Name, item.entity2ID,item.entity2Name,item.topicID,item.insertTime))

    lazy val remen_schema = StructType(
      StructField("id", IntegerType) :: StructField("type", StringType) :: StructField("entity1mention_id", IntegerType) ::
        StructField("entity1_name", StringType) :: StructField("entity2mention_id", IntegerType) ::
        StructField("entity2_name", StringType) :: StructField("Hbase_id", StringType) ::
        StructField("topic_id", StringType) :: StructField("post_time", StringType) ::
        StructField("insert_time", StringType) :: Nil
    )
    lazy val re_schema = StructType(
      StructField("id", IntegerType) ::StructField("type", StringType) ::
        StructField("entity1_id", IntegerType) :: StructField("entity1_name", StringType) ::
        StructField("entity2_id", IntegerType) :: StructField("entity2_name", StringType) ::
        StructField("topic_id", IntegerType) :: StructField("insert_time", StringType) :: Nil
    )
    lazy val remen_df = sqlContext.createDataFrame(remen_rdd, remen_schema)
    lazy val re_df = sqlContext.createDataFrame(re_rdd, re_schema)
    remen_df.write.mode(SaveMode.Append).jdbc(url, relationmention, property)
    re_df.write.mode(SaveMode.Append).jdbc(url, relation, property)
    println("sucessful!!!")



    //--------------------------------------------------------
    def initialmatrix(x: String): Iterable[(RelationMention, Array[Int])] = {
      var illegal = 0
      //      println("The HbaseID now dealt is " + x)
      val HbaseIDEntityListx = sameHbaseIDs.get(x)
      var HbaseIDEntityList = Array[EntityMention]()
      if(!HbaseIDEntityListx.isEmpty) HbaseIDEntityList = HbaseIDEntityListx.get.toArray
      var myRela_Candia = new HashMap[RelationMention, Array[Int]]()

      //      println("In this text(HbaseID),we have " + HbaseIDEntityList.size + " Entities in total!")

      //get sentences from the text(HbaseID)

      var punc = ArrayBuffer[String]()
      punc +=("，", ".", "?", ";", "!", ",", "。", "？", "；", "！")
      val puncStr = "。"
      //      var ALLSentences = Array[String]()
      var sentences = new Array[Array[String]](2)
      //      println("x:"+ x)
      val ALLSentences = textList.get(x)

      val content_puc = ALLSentences.get(1)

      //split content to Array with punctuations and cut space and \n

      val content = content_puc.replaceAll("[\t]", "").replaceAll("[\r\n]", "_")
      //          println("content:" + content)
      val title = ALLSentences.get(0).replaceAll("[\t]", "").replaceAll("[\r\n]", "_")
      var tempList = new ArrayBuffer[String]()
      val puncPositonList = new ArrayBuffer[Int]()
      val sentenceList = content.split("[，.?;!,。？；！]+")
      val titleList = title.split("[，.?;!,。？；！]+")
      sentences(0) = titleList
      sentences(1) = sentenceList

      if (HbaseIDEntityList.size == 1 || HbaseIDEntityList.isEmpty) {
        illegal = 1
      }

      for (i <- 0 to punc.size - 1) {
        HbaseIDEntityList.foreach {
          x =>
            if (x.name.contains(punc(i))) illegal = 1
            if (x.senOffset.toInt < 0) illegal = 1
        }
      }
      if (illegal == 0) {
        myRela_Candia = detectRelation(HbaseIDEntityList, sentences) //dealt entitys of every text(HbaseID)
      }
      myRela_Candia.toIterable
    }
    //--------------------------------------------------------

    def detectRelation(HbaseIDEntityList: Array[EntityMention],
                       sentences: Array[Array[String]]): HashMap[RelationMention, Array[Int]] = {
      //      val positionMapRDD = HbaseIDEntityRDD.map(x =>
      //        Map(x.entitymention_id.toInt -> Array(x.senOffset.toInt,x.senOffset.toInt + x.name.length-1)))
      var mymatrix = ArrayBuffer[Array[Int]]()
      val HbaseIDEntityList_size = HbaseIDEntityList.size
      var Rela_Candia = new HashMap[RelationMention, Array[Int]]
      for (i <- 0 to HbaseIDEntityList_size - 1) {
        for (j <- i + 1 to HbaseIDEntityList.size - 1) {
          if (!(HbaseIDEntityList(j).name == HbaseIDEntityList(i).name)) {
            //not the same entity
            if (HbaseIDEntityList(j).post_type == HbaseIDEntityList(i).post_type &&
              HbaseIDEntityList(j).psenIndex == HbaseIDEntityList(i).psenIndex) {
              //in the same sentence
              //              println("k.senIndex" + HbaseIDEntityList(j).psenIndex + "---" + "y.senIndex" +
              //                HbaseIDEntityList(i).psenIndex)
              val postype = HbaseIDEntityList(i).post_type.toInt
              val senindex = HbaseIDEntityList(i).psenIndex.toInt
              var pore = 0
              var sentence: String = ""
              if (senindex < sentences(postype - 1).size) {
                sentence = sentences(postype - 1)(senindex)
              }
              //              println("the sentence now dealting is:" +sentence)
              //              println("the entity now dealting is:" +HbaseIDEntityList(i).name +
              //                " and " + HbaseIDEntityList(j).name)
              if (!sentence.equals("") && sentence.length < 100) {
                val entity1Positon = Array(HbaseIDEntityList(i).senOffset.toInt,
                  HbaseIDEntityList(i).senOffset.toInt + HbaseIDEntityList(i).name.length - 1)
                val entity2Positon = Array(HbaseIDEntityList(j).senOffset.toInt,
                  HbaseIDEntityList(j).senOffset.toInt + HbaseIDEntityList(j).name.length - 1)

                //calculate position of entity pairs

                val info = calculatePosition(entity1Positon, entity2Positon, sentence)
                val a = info.keys.toArray
                pore = a(0)

                //save  candidate relations

                val tmpRelation: RelationMention = new RelationMention(key, "type", HbaseIDEntityList(i).entitymention_id.toInt,
                  HbaseIDEntityList(i).name, HbaseIDEntityList(j).entitymention_id.toInt, HbaseIDEntityList(j).name,
                  HbaseIDEntityList(i).HbaseID, HbaseIDEntityList(i).topicID.toInt,
                  HbaseIDEntityList(i).insertTime, HbaseIDEntityList(i).postTime)
                //                candidateList += tmpRelation
                key += 1

                //calculate Sempair of entity pairs

                val sempairTheTwo: String = calculateSempair(HbaseIDEntityList(i).entitymention_type,
                  HbaseIDEntityList(j).entitymention_type)
                //                println("sempairTheTwo:"+sempairTheTwo)
                val p = info.values.toArray
                val binList = p(0)

                //build the feature vector of entity pairs

                val vector = buildFeatureVector(pore, sempairTheTwo, binList).toArray
                Rela_Candia += ((tmpRelation, vector))
              }
            }
          }
        }
      }
      Rela_Candia
    }

    def calculatePosition(entity1Positon: Array[Int], entity2Positon: Array[Int],
                          sentence: String): HashMap[Int, Array[String]] = {
      var bin = new ArrayBuffer[String]
      var info = new HashMap[Int, Array[String]]
      val start1 = entity1Positon(0)

      val end1 = entity1Positon(1)
      val start2 = entity2Positon(0)
      val end2 = entity2Positon(1)

      var pore = 0
      if (end1 < start2) {
        //entity1 is before entity2
        pore = 1
        bin += sentence.substring(0, start1)
        bin += sentence.substring(start1, end1 + 1)
        bin += sentence.substring(end1 + 1, start2)
        if (end2 == (sentence.length - 1)) {
          bin += sentence.substring(start2)
        } else {
          bin += sentence.substring(start2, end2 + 1)
          bin += sentence.substring(end2 + 1)
        }
      } else if (start1 > end2) {
        //entity1 is behind entity2
        pore = 2
        bin += sentence.substring(0, start2)
        bin += sentence.substring(start2, end2 + 1)
        bin += sentence.substring(end2 + 1, start1)
        if (end2 == (sentence.length - 1)) {
          bin += sentence.substring(start1)
        } else {
          bin += sentence.substring(start1, end1 + 1)
          bin += sentence.substring(end1 + 1)
        }
      } else if (start1 <= start2 && end1 >= end2) {
        //entity1 include entity2
        pore = 3
        bin += sentence.substring(0, start1)
        if (end1 == (sentence.length - 1)) {
          bin += sentence.substring(start1)
          if (end2 == sentence.length - 1) {
            bin += sentence.substring(start2)
          } else {
            bin += sentence.substring(start2, end2 + 1)
          }
        } else {
          bin += sentence.substring(start1, end1 + 1)
          bin += sentence.substring(start2, end2 + 1)
          bin += sentence.substring(end1 + 1)
        }
      } else {
        //entity2 include entity1
        pore = 4
        bin += sentence.substring(0, start2)
        if (end2 == (sentence.length - 1)) {
          bin += sentence.substring(start2)
          if (end1 == sentence.length - 1) {
            bin += sentence.substring(start1)
          } else {
            bin += sentence.substring(start1, end1 + 1)
          }
        } else {
          bin += sentence.substring(start2, end2 + 1)
          bin += sentence.substring(start1, end1 + 1)
          bin += sentence.substring(end2 + 1)
        }
      }
      info += ((pore, bin.toArray))
      info
    }

    def calculateSempair(entity1Type: String, entity2Type: String): String = {
      var type1 = ""
      var type2 = ""

      entity1Type match {
        case "1" => {
          type1 = "PER"
        }
        case "2" => {
          type1 = "ORG"
        }
        case "3" => {
          type1 = "LOC"
        }
      }

      entity2Type match {
        case "1" => {
          type2 = "PER"
        }
        case "2" => {
          type2 = "ORG"
        }
        case "3" => {
          type2 = "LOC"
        }
      }
      val Sempair = type1 + "_" + type2
      Sempair
    }

    def buildFeatureVector(pore: Int, sempair: String, binlist: Array[String]) = {
      var vector = new ArrayBuffer[Int]
      vector += 1
      vector += pore //add position feature
      for (i <- 0 to sempairList.size - 1) {
        //add sempair feature
        if (sempairList(i).equals(sempair))
          vector += (i + 5)
      }
      for (i <- 0 to binlist.size - 1) {
        if (binlist(i) != null) {
          val ss = binlist(i).replaceAll("\\s|\n| ", "") //drop space and huanhang
          for (j <- 0 to ss.length - 1) {
            var string = ""
            for (k <- j to ss.length - 1) {
              string = string + ss(k)
              if (dic_buffer.contains(string)) {
                val binString = string + '_' + "Bin" + i
                for (l <- 0 to binsen.size - 1) {
                  if (binString.equals(binsen(l)))
                    vector += (l + 5 + sempairList.size)
                }
              }
            }
          }
        }
      }
      vector
    }

    def modifyMatrix() = {
      val path = input_Hbase_w
      val modifyMatrix = new ArrayBuffer[String]()
      for (i <- 0 to matrix.size - 1) {
        var String: String = matrix(i)(0).toString + " "
        for (j <- 1 to matrix(i).size - 1) {
          String += matrix(i)(j).toString + ":1" + " "
        }
        modifyMatrix += String
      }
      val modifyRDD = sc.parallelize(modifyMatrix)

      //delete temp input_hbase

      val conf = new Configuration()
      val hdfsCoreSitePath = new Path("projectFile/core-site.xml")
      val hdfsHDFSSitePath = new Path("projectFile/hdfs-site.xml")
      conf.addResource(hdfsCoreSitePath)
      conf.addResource(hdfsHDFSSitePath)
      val fileSystem = FileSystem.get(conf)
      if(fileSystem.exists(new Path(path)))
        fileSystem.delete(new Path(path),true)

      modifyRDD.coalesce(1).saveAsTextFile(path)
    }

    def predicInput() = {
      var args = new Array[String](10)
      args(0) = "-b"
      args(1) = "1"
      args(2) = input_Hbase_r
      args(3) = Model
      args(4) = Out
      Predict.main(args)

      val out = sc.textFile(Out)
      var predictList = new ArrayBuffer[Int]()
      var weightList = new ArrayBuffer[Double]()
      val firstline = out.first().split(" ")
      var label = new HashMap[Int, Int]()
      val labels = new Array[Int](6)
      for (i <- 1 to firstline.size - 1) {
        labels(i - 1) = firstline(i).toInt
        label += ((firstline(i).toInt, i))
      }
      var content = ""
      var o = out.map(x => x.split(" ")).filter(x => x(0) != "labels").collect()
      o.foreach(x => {
        if (x(0).toDouble == -1) {
          if (x(1).toDouble < 0.95) {
            var prob: Double = 0
            var index = 0
            for (i <- 0 to 4) {
              if (x(i + 2).toDouble > prob) {
                prob = x(i + 2).toDouble
                index = i + 2
              }
            }
            predictList += labels(index - 1)
            weightList += prob
          } else {
            predictList += x(0).toDouble.toInt
            weightList += x(label.apply(x(0).toDouble.toInt)).toDouble
          }
        } else {
          predictList += x(0).toDouble.toInt
          weightList += x(label.apply(x(0).toDouble.toInt)).toDouble
        }
      })

      var id = sqlContext.read.format("jdbc").options(
        Map("url" -> url, "user" -> user, "password" -> password,
          "dbtable" -> relationmention)).load().repartition(24).count().toInt + 1

      for (i <- 0 to predictList.size - 1) {
        if (predictList(i) > 0) {
          var relaType = ""
          predictList(i) match {
            case 1 => {
              relaType = "社交事务"
            }
            case 2 => {
              relaType = "包含构成"
            }
            case 3 => {
              relaType = "从属依附"
            }
            case 4 => {
              relaType = "地理位置"
            }
            case 5 => {
              relaType = "雇佣聘用"
            }
          }
          candidateList(i).id = id
          candidateList(i).relationType = relaType
          relation_mentionList += candidateList(i)
          id += 1
        }
      }
      println("Classification has been completed, ready to insert the database!")
    }


    def generateRelation() = {
      var eventidSet = new HashSet[Int]()
      var i = 0

      for (i <- 0 to relation_mentionList.size - 1) {
        var tempList = new ArrayBuffer[RelationMention]()
        var id = relation_mentionList(i).topicID
        if (!eventidSet.contains(id)) {
          for (j <- 0 to relation_mentionList.size - 1) {
            if (relation_mentionList(j).topicID == id) {
              tempList += relation_mentionList(j)
            }
          }
          eventidSet += id
          aggregateRelation(tempList)
        }
      }
      var relationList = relationListRDD.collect()
      var Relation_id = sqlContext.read.format("jdbc").options(
        Map("url" -> url, "user" -> user, "password" -> password,
          "dbtable" -> relation)).load().repartition(24).count().toInt + 1
      relationList.foreach(x => {
        x.relationID = Relation_id
        Relation_id = Relation_id + 1
      })
//      println("relationList.count()"+relationListRDD.count)
      relationList
    }

    def aggregateRelation(tempList: ArrayBuffer[RelationMention]): Unit = {var reid = new HashSet[Int]()
      val colll=for (k <- 0 to tempList.size - 1) yield {
        val retype1 = tempList(k).relationType
        val entity1_name1 = tempList(k).entity1Name
        val entity2_name1 = tempList(k).entity2Name
        var aList = new ArrayBuffer[RelationMention]()
        var re: Relation = null
        if (!reid.contains(tempList(k).id)) {
          for (l <- 0 to tempList.size - 1) {
            val retype2 = tempList(l).relationType
            val entity1_name2 = tempList(l).entity1Name
            val entity2_name2 = tempList(l).entity2Name
            if ((retype2.equals(retype1) && entity1_name2.equals(entity1_name1) && entity2_name2.equals(entity2_name1)) ||
              (retype2.equals(retype1) && entity1_name2.equals(entity2_name1) && entity2_name2.equals(entity1_name1))) {
              aList += tempList(l)
              reid += tempList(l).id
            }
          }
          val myRelaMen = aList(0)
          Option(myRelaMen)
        }
        else
          Option.empty
      }
      val coOfRelationmention= sc.parallelize(colll.filter(!_.isEmpty))
      //RDD[(Int, (Option[RelationMention], EntityMention))]
      //     (ID , (_ ,entity1,))
      val entity1=coOfRelationmention.keyBy(_.get.entitymention1ID)
        .join(entitymentions.keyBy(_.entitymention_id.toInt)).reduceByKey{(a,b)=>a}.values
      // RDD[((Option[RelationMention], EntityMention), EntityMention)]
      //((_,entity1),entity2)
      val entity2=entity1.keyBy(_._1.get.entitymention2ID)
        .join(entitymentions.keyBy(_.entitymention_id.toInt)).reduceByKey{(a,b)=>a}.values
      relationListRDD=entity2.filter(r => r._1._1.get.topicID>=0).map[Relation]{
        case((myRelaMen_pro,entity1),entity2)=>
          val myRelaMen=myRelaMen_pro.get
            val re_topicID = myRelaMen.topicID
            val re_relationType = myRelaMen.relationType
            new Relation(1,re_relationType, entity1.entityID.toInt, entity1.name, entity2.entityID.toInt,
              entity2.name, re_topicID, "")
      }.union(relationListRDD)
    }
//    sc.stop()
  }


}
