package org.apache.scala.abnormality

/**
 * Created by liumeng on 15-11-20.
 */

import java.sql.Timestamp
import java.util.Properties
import org.apache.lib.Predict
import org.apache.spark.sql.{SaveMode, SQLContext}
import org.apache.spark.{SparkConf, SparkContext}
import scala.collection.mutable.ArrayBuffer

import org.apache.hadoop.fs.Path
import org.apache.hadoop.conf._
import org.apache.hadoop.fs._

case class abn(id:Int,_trigger:String,actiontype:Int,topic_id:String,sentence:String, entity:String,url:String,post_time:Timestamp)
object Detect_ab {
  def do_abnormalDetect(Prefix_path:String, dburl:String, dbuser:String, dbpwd:String,sc:SparkContext,sqlContext:SQLContext) {

    //val sparkConf = new SparkConf().setAppName("yichangxingwei")
    //val sc = new SparkContext(sparkConf)

    //val sc = new SparkContext("local","app")
    // val sqlContext = new SQLContext(sc)

    //liblinear包里predict函数参数
    val input_Hbase = Prefix_path + "predict_input"//"hdfs://namenode1:9000/liu/projectFile/predict_input1"
    val Model = Prefix_path + "Model.model"//"hdfs://namenode1:9000/liu/projectFile/Model.model"
    val Out = Prefix_path + "out.txt"//hdfs://test:9000/liumeng/out1.txt

    //post_topic表数据
    val post_topicDF = sqlContext.read.format("jdbc").options(
      Map("url" -> dburl, "user" -> dbuser, "password" -> dbpwd, "dbtable" -> "post_topic")).load()
    val post_topic_data = post_topicDF.map { row => (row.getString(row.fieldIndex("post_id")),
      (row.getInt(row.fieldIndex("topic_id"))))
    }.cache()
    val post_id_list = post_topic_data.map { case (x, y) => x }.collect()
    val post_topic_array = post_topic_data.collect()

    //post表数据
    val postDF = sqlContext.read.format("jdbc").options(
      Map("url" -> dburl, "user" -> dbuser, "password" -> dbpwd, "dbtable" -> "post")).load()
    val post_data = postDF.filter("choice=1").filter("language_type=0").map {
      row => ((row.getString(row.fieldIndex("id"))),
        (row.getString(row.fieldIndex("url"))),
        (row.getTimestamp(row.fieldIndex("post_time"))),
        (row.getString(row.fieldIndex("content")).replaceAll("[\t]", "").replaceAll("[\r\n]", "")
          .replaceAll(" ","").replaceAll("　","")))
    }.cache()

    //在post_topic表里有记录的post表数据
    val can_post = post_data.filter { s => {
      var ff = false
      for (jk <- 0 to post_id_list.length - 1)
        if (s._1 == post_id_list(jk))
          ff = true
      ff
    }
    }

    //entitymention表数据
    val entitymentionDF = sqlContext.read.format("jdbc").options {
      Map("url" -> dburl, "user" -> dbuser, "password" -> dbpwd, "dbtable" -> "entitymention")
    }.load()
    val entity_data = entitymentionDF.map { row => ((row.getString(row.fieldIndex("name"))),
      (row.getString(row.fieldIndex("Hbase_id"))),
      (row.getInt(row.fieldIndex("sen_index"))))
    }
    val HbaseIDs = entity_data.map(x => x._2).distinct().collect()
    val sameHbaseIDs = entity_data.groupBy(x => x._2).collect().toMap

    //读取触发词表，保存仅含中文的触发词表
    val anchlist = sc.textFile(Prefix_path + "anchordict.txt").map(line => line.split(" ", 2)).map { line => line(0) }.collect()
    //读入词典，抽取特征向量
    val dictionary = sc.textFile(Prefix_path + "temp.txt").flatMap(line => line.split(",")).collect()

    //重写部分
    val anch_table = ArrayBuffer[String]() //句子所含触发词列表,默认一句检测到一个触发词
    val sentence_list = ArrayBuffer[sentence]()
    val nosense = can_post.map { p => (p._1, p._2, p._3, p._4.split("[。；？！.;?!]")) }.collect()
    for (i <- 0 to nosense.length - 1) {
      var index_sample = 0
      for (dk <- 0 to nosense(i)._4.length - 1) {
        var indexlist = new ArrayBuffer[Int]() //chang句子所含子句的index值列表
        for( index_seq <- 0 to nosense(i)._4(dk).split("[,，]").length-1)
          {
            indexlist += index_sample
            index_sample = index_sample + 1
          }
        var ck = 0
        var dd = false
        while (dd == false && ck < anchlist.length) {
          if (nosense(i)._4(dk).contains(anchlist(ck))) {

            var entitylist: String = ""
            var flag = false
            var tok = 0
            while(flag == false && tok < HbaseIDs.length)
            {
              if(nosense(i)._1 == HbaseIDs(tok))
                flag = true
              tok = tok + 1
            }
            if(flag == true)
            {
              val en_array = sameHbaseIDs.apply(nosense(i)._1).toArray
              for(pm <- 0 to en_array.length-1)
                for(pn <- 0 to indexlist.length-1)
                  {
                    if(en_array(pm)._3 == indexlist(pn))
                      entitylist += (en_array(pm)._1 + ";")
                  }
            }

            anch_table += anchlist(ck)
            dd = true
            sentence_list += new sentence(nosense(i)._1, nosense(i)._2, nosense(i)._3, nosense(i)._4(dk), entitylist)
          }
          ck += 1
        }
      }
    }

    val feature_rdd = sc.parallelize(sentence_list).map { a => {
      //特征向量RDD
      var str = "-1"
      for (ijk <- 0 to dictionary.length - 1 if a.sentence.contains(dictionary(ijk))) yield {
        str = str + " " + ijk + ":1"
      }
      str
    }
    }.repartition(1).cache()

    val conf = new Configuration()
    val hdfsCoreSitePath = new Path("./projectFile/core-site.xml")
    val hdfsHDFSSitePath = new Path("./projectFile/hdfs-site.xml")

    conf.addResource(hdfsCoreSitePath)
    conf.addResource(hdfsHDFSSitePath)

    val fileSystem = FileSystem.get(conf)

    if(fileSystem.exists(new Path(input_Hbase)))
      fileSystem.delete(new Path(input_Hbase), true) // true for recursive
    feature_rdd.saveAsTextFile(input_Hbase)
    //println("特征向量文件输出")

    //Predict函数参数列表
    var args = new Array[String](10)
    args(0) = "-b"
    args(1) = "1"
    args(2) = input_Hbase + "/part-00000"
    args(3) = Model
    args(4) = Out
    val pre = new Predict
    pre.ExecPredict(args)

    val out = sc.textFile(Out).filter(x => !x.contains("labels")).map(x => x.split(" ", 2)).map(x => x(0)).collect()

    var m = 1 //abnormality表的id
    var k = 0
    val abnormal_list = new ArrayBuffer[abnormality]() //存储待写入abnormality表的数据
    out.foreach { symbol => {
      if (symbol != "-1.00000") {
        val id = m
        val post_time = sentence_list(k).post_time
        val _trigger = anch_table(k)
        val sentence = sentence_list(k).sentence
        val url = sentence_list(k).url

        var zip = 0
        while(post_topic_array(zip)._1 != sentence_list(k).id && zip < post_topic_array.length-1)
        {
          zip += 1
        }
        val topics = post_topic_array(zip)._2.toString
        symbol match {
          case "1.00000" => abnormal_list += new abnormality(id, post_time, _trigger, sentence, 1, topics, sentence_list(k).entitys, url)
          case "2.00000" => abnormal_list += new abnormality(id, post_time, _trigger, sentence, 2, topics, sentence_list(k).entitys, url)
          case "3.00000" => abnormal_list += new abnormality(id, post_time, _trigger, sentence, 3, topics, sentence_list(k).entitys, url)
          case "4.00000" => abnormal_list += new abnormality(id, post_time, _trigger, sentence, 4, topics, sentence_list(k).entitys, url)
        }
        m += 1
        k += 1
      }
      else
        k += 1
    }
    }
//println(abnormal_list.length)

    //处理完，开始写入数据库
    import sqlContext.implicits._
    val abnorrdd = sc.parallelize(abnormal_list,4)
    val abnorDF = abnorrdd.map { p => abn(p.id, p._trigger, p.actiontype, p.topic_id,
      p.sentence, p.entity, p.url, p.post_time) }.toDF()
    val property = new Properties()
    val url = dburl+"?useUnicode=true&characterEncoding=utf-8"
    property.put("user", dbuser)
    property.put("password", dbpwd)
    abnorDF.write.mode(SaveMode.Append).jdbc(url, "abnormality", property)

//    sc.stop()
  }
}



