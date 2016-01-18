package org.apache.scala.entity_extract
import java.sql.Timestamp

/**
  * Created by wyy on 11/25/15.
  */
class Entity extends Serializable{
  var entity_id:Int = -1
  var name:String = null
  var entity_type:Int = -1
  var value:Float = -1
  var start_time:Timestamp= null
  var end_time:Timestamp = null
  var insert_time:Timestamp = null
  var topic_id:Int = -1


}
