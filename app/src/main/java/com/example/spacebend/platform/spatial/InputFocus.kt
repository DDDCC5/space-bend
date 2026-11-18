package com.example.spacebend.platform.spatial

/** Keeps the displayed ray on the hand the player is using. */
internal class InputFocus {
 private var selectedId: String?=null
 private var engagedIds=emptySet<String>()
 private var actionMasks=emptyMap<String,Int>()
 private fun actionMask(input: InputFrame)=(if(input.trigger)1 else 0) or (if(input.grip)2 else 0) or (if(input.delete)4 else 0) or (if(input.stickClick)8 else 0) or (if(kotlin.math.abs(input.stickX)>.65f)16 else 0)
 private fun engaged(input: InputFrame)=input.trigger||input.grip||input.delete||input.stickClick||kotlin.math.abs(input.stickX)>.3f||input.aimActive
 fun choose(inputs: List<InputFrame>,capturedId: String?=null): InputFrame? {
  val selected=inputs.firstOrNull{it.id==selectedId}
  val chosen=inputs.firstOrNull{it.id==capturedId}
   ?:inputs.lastOrNull{actionMask(it) and (actionMasks[it.id]?:0).inv()!=0}
   ?:selected?.takeIf{actionMask(it)!=0}
   ?:inputs.lastOrNull{engaged(it)&&it.id !in engagedIds}
   ?:selected?.takeIf(::engaged)
   ?:inputs.lastOrNull(::engaged)
   ?:selected
   ?:inputs.firstOrNull()
  selectedId=chosen?.id;engagedIds=inputs.filter(::engaged).map{it.id}.toSet()
  actionMasks=inputs.associate{it.id to actionMask(it)}
  return chosen
 }
 fun reset(){selectedId=null;engagedIds=emptySet();actionMasks=emptyMap()}
}
