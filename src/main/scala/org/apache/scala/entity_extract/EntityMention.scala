package org.apache.scala.entity_extract
/**
  * Created by wyy on 11/25/15.
  */
import java.sql.{Timestamp, DriverManager, Connection}
class EntityMention extends Serializable{
  var id:Int = -1
  var name:String = ""
  var entity_type:Int = -1
  var Hbase_id:String = ""
  var topic_id:Int = -1
  var text_src:Int = -1
  var post_type:Int = -1
  var offset_start:Int = -1
  var offset_end:Int = -1
  var sen_index:Int = -1
  var sen_offset:Int = -1
  var post_time:Timestamp= null
  var update_time:Timestamp = null
  var insert_time:Timestamp = null
  //var entity_id:Int = -1

}
