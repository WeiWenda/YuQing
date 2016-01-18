/**
 * Created by luan on 15-11-20.
 */

package org.apache.scala.relation_detect

class EntityMention(val pentitymention_id:String,var pname:String,var pentitymention_type:String,
                    var pHbaseID:String,var ptopicID:String,var ppost_type:String,
                    var psenIndex:String,var psenOffset:String, var ppostTime:String,
                    var pinsertTime:String,var pentityID:String) extends Serializable {
  var entitymention_id = pentitymention_id
  var name = pname
  var entitymention_type = pentitymention_type
  var HbaseID = pHbaseID
  var topicID= ptopicID

  var post_type = ppost_type

  var senIndex = psenOffset
  var senOffset = psenOffset
  var postTime = ppostTime

  var insertTime = pinsertTime
  var entityID = pentityID

}
