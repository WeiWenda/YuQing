package org.apache.scala.AppMain

import java.util.Date
import java.text.SimpleDateFormat

import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.scala.topic_discover.TopicDiscover
import org.apache.scala.entity_extract.Entrance
import org.apache.scala.relation_detect.Relation_Detector
import org.apache.scala.abnormality.Detect_ab
/**
 * Created by david on 1/13/16.
 */
object YuQing_By_XJTUCloud {

  def main (args: Array[String]) {
    val sparkConf = new SparkConf().setAppName("YuQing") // An existing SparkContext.
    val sc = new SparkContext(sparkConf)
    val sqlContext = new SQLContext(sc)
    val dbIP="192.168.199.35"
    val databaseName="cloudyuqing"
    val dbuser="root"
    val dbpwd="root"
    val format = new SimpleDateFormat("hh:mm:ss.SSS")
    //Print For Debug------------------
    val topicDiscover_start=new Date()
    //---------------------------------
    TopicDiscover.do_TopicDiscover("hdfs://namenode1:9000/",dbIP,databaseName,dbuser,dbpwd,sc,sqlContext)
    //Print For Debug------------------
    val entityExtract_start=new Date()
    //---------------------------------
    Entrance.do_entityExtract("jdbc:mysql://"+dbIP+":3306/"+databaseName+"?useUnicode=true&characterEncoding=utf-8",dbuser,dbpwd,sc,sqlContext)
    //Print For Debug------------------
    val abnormalityDetect_start=new Date()
    //---------------------------------
//    Detect_ab.do_abnormalDetect("hdfs://namenode1:9000/liu/projectFile/","jdbc:mysql://"+dbIP+":3306/"+databaseName,dbuser,dbpwd,sc,sqlContext)
    //Print For Debug------------------
    val relationDetect_start=new Date()
    //---------------------------------
    Relation_Detector.do_relationDetect("hdfs:///home/skyclass/","jdbc:mysql://"+dbIP+":3306/"+databaseName,dbuser,dbpwd,sc,sqlContext)
    //Print For Debug------------------
    sc.stop()
    val app_end=new Date()
    println("\r"+format.format(topicDiscover_start)+"\tTopicDiscover Starting!")
    println("\r"+format.format(entityExtract_start)+"\tEntityExtract Starting!")
    println("\r"+format.format(abnormalityDetect_start)+"\tabnormalityDetect Starting!")
    println("\r"+format.format(relationDetect_start)+"\tRelationDectect Starting!")
    println("\r"+format.format(app_end)+"\tAll Completed!")
    //---------------------------------

  }
}
