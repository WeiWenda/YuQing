/**
 * Created by luan on 15-11-30.
 */
package org.apache.scala.relation_detect

class Relation (val prelationType:String,val pentity1ID:Int,val pentity1Name:String,
                       val pentity2ID:Int,val pentity2Name:String,val ptopicID:Int,
                       val pinsertTime:String) extends Serializable {
  var relationType = prelationType
  var entity1ID = pentity1ID
  var entity1Name = pentity1Name
  var entity2ID = pentity2ID
  var entity2Name = pentity2Name
  var topicID = ptopicID
  var insertTime = pinsertTime
}