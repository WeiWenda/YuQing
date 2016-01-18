package org.apache.scala.entity_extract
/**
  * Created by wyy on 11/20/15.
  */

import java.io._
import java.sql.{PreparedStatement, Timestamp, DriverManager, Connection}
import java.util.Properties

import com.mysql.jdbc.Driver
import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, SaveMode, SQLContext}

import scala.collection.mutable.ArrayBuffer


object Merge {
  var topic: Iterable[(Int, String, Int, Int, Timestamp, Timestamp, String)] = _

  case class en(entity_id:Int,name: String, entity_type: Int,topic_id:Int,value:Float,start_time:Timestamp,end_time:Timestamp,insert_time:Timestamp)

  def merge(sqlContext:SQLContext,sc:SparkContext,jdbcDF:DataFrame,topic_table:DataFrame,DBurl:String,DBuser:String,DBpwd:String,entable:String):DataFrame = {

    var id_num:Int = 1
/*
    val jdbcDF = sqlContext.read.format("jdbc").options(
      Map("url" -> "jdbc:mysql://202.117.16.118:3306/net","user"->"wyy","password"->"wyy",
        "dbtable" -> "entitymention")).load()
    //jdbcDF.foreach(c=>println(c))
    //val entity_list = jdbcDF.collectAsList()
    //print(jdbcDF)
    //val jdbcrdd = jdbcDF.map(q=>(q,1)).map{case (w,n)=>w}
    //jdbcrdd.foreach(c=>println(c))
*/
    val data = jdbcDF.map(row =>
      (row.getInt(row.fieldIndex("id")),
        row.getString(row.fieldIndex("name")),
        row.getInt(row.fieldIndex("topic_id")),
        row.getInt(row.fieldIndex("entity_type")),
        row.getTimestamp(row.fieldIndex("post_time")),
        row.getTimestamp(row.fieldIndex("insert_time")),
        row.getString(row.fieldIndex("Hbase_id"))

      )
    )

    //data.foreach(c=>println(c))
/*
    var conn:Connection = null
    val d :Driver = null
    var pstmt:PreparedStatement = null
    val url="jdbc:mysql://202.117.16.118:3306/net?useUnicode=true&characterEncoding=utf-8"
    val user="wyy"
    val password="wyy"
    conn = DriverManager.getConnection(url, user, password)
*/
    //val totalArticalNum = data.groupBy(x=>x._7).collect().length

    var entity_list:ArrayBuffer[Entity] = new ArrayBuffer[Entity]()

    val groupByTopic = data.groupBy(x=>x._3).map{case (a,b)=>b}.collect()//.foreach(c=>println(c))
    //groupByTopic(0).groupBy(x=>x._2).foreach(c=>println(c))
    for (i <- 0 to groupByTopic.size-1) {
      topic = groupByTopic(i)
      var totalArticalNum = groupByTopic(i).groupBy(x=>x._7).size
      //println(topic.head._3+"-------------------------")
      val eachEntity = groupByTopic(i).groupBy(x => x._2).map { case (a, b) => b }
      //calculate value
      val y = eachEntity.toList
      //var entity:Entity = new Entity()

      y.foreach(
        x=>{
          val eachList0 = x.groupBy(f=>f._4).map { case (a, b) => b }.toList
          eachList0.foreach(q=>{
            var eachList = q.toArray
          var latest:Timestamp = eachList(0)._5
          var earliest:Timestamp = eachList(0)._5

          var tf = eachList.length
          var articalNum:Double = eachList.groupBy(x=>x._7).toArray.length
          var df:Double = 1 + articalNum/(totalArticalNum.toDouble)
          var value:Float = (tf * Math.exp(df)).toFloat

        for(j <- 0 to eachList.size-1){
          if(latest.before(eachList(j)._5)) latest = eachList(j)._5
          if(earliest.after(eachList(j)._5)) earliest = eachList(j)._5
        }

          var entity:Entity = new Entity()
          var cur_time:Timestamp = new Timestamp(System.currentTimeMillis())
          entity.entity_id = id_num
        entity.name = eachList.head._2
        entity.topic_id = eachList.head._3
        entity.entity_type = eachList.head._4
          entity.start_time = earliest
        entity.end_time = latest
          entity.insert_time = cur_time
          entity.value = value

          entity_list += entity

          id_num += 1
        //InsertDB.insertEntity(entity)

          //println(tf+" "+articalNum+" "+df+" "+entity.name+" "+entity.entity_type+" "+entity.topic_id+" "+entity.start_time+" "+entity.end_time+" "+entity.value+" "+totalArticalNum)
        })}
      )

    }

    println(id_num+"-------EN Start----------")
    import sqlContext.implicits._
    val enrdd = sc.parallelize(entity_list,24)
    val enDF = enrdd.map{p=> en(p.entity_id,p.name,p.entity_type,p.topic_id,p.value,p.start_time,p.end_time,p.insert_time)}.toDF()
    val property = new Properties()
    val url = DBurl
    property.put("user",DBuser)
    property.put("password", DBpwd)
    topic_table.registerTempTable("topic_table")
    enDF.registerTempTable("en_table")
    val aa = sqlContext.sql("select entity_id,name,entity_type,en_table.topic_id,value,start_time,end_time,insert_time from en_table,topic_table where en_table.topic_id in (topic_table.topic_id)")
    aa.write.mode(SaveMode.Append).jdbc(url, entable, property)
    println(id_num+"-------EN End----------")

    return enDF

  }

}
