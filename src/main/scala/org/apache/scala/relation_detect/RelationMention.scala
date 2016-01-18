/**
 * Created by luan on 15-11-26.
 */

package org.apache.scala.relation_detect

class RelationMention (val pid:Int,val prelationType:String,val pentitymention1ID:Int,val pentity1Name:String,
                val pentitymention2ID:Int,val pentity2Name:String,val pHbaseID:String,val ptopicID:Int,
                 val pinsertTime:String,val ppostTime:String) extends Serializable {
  var id = pid
  var relationType = prelationType
  val entitymention1ID = pentitymention1ID
  val entity1Name = pentity1Name
  val entitymention2ID = pentitymention2ID
  val entity2Name = pentity2Name
  val HbaseID = pHbaseID
  val topicID = ptopicID
  val insertTime = pinsertTime
  val postTime = ppostTime
}
