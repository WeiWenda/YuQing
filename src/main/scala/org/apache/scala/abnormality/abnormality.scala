package org.apache.scala.abnormality

import java.sql.Timestamp

/**
 * Created by david on 11/23/15.
 */
class abnormality (val aid:Int,val apost_time:Timestamp,val a_trigger:String,val asentence:String,val aaction_type:Int,
                    val atopic_id:String,val aentity:String,val aurl:String) extends Serializable{
  var id: Int = aid
  var post_time: Timestamp = apost_time
  var _trigger: String = a_trigger
  var sentence: String = asentence
  var actiontype: Int = aaction_type
  var topic_id: String = atopic_id
  var entity: String = aentity
  var url: String = aurl
}
