package org.apache.scala.abnormality

import java.sql.Timestamp

/**
 * Created by liumeng on 15.12.15.
 */
class sentence (val sid:String, val surl:String, val spost_time:Timestamp, val ssentence:String, val sentity:String) extends Serializable{
  var id: String = sid
  var post_time: Timestamp = spost_time
  var sentence: String = ssentence
  var url: String = surl
  var entitys: String = sentity
}
