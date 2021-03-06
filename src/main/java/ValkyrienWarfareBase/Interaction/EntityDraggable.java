package ValkyrienWarfareBase.Interaction;

import ValkyrienWarfareBase.ValkyrienWarfareMod;
import ValkyrienWarfareBase.API.RotationMatrices;
import ValkyrienWarfareBase.API.Vector;
import ValkyrienWarfareBase.PhysicsManagement.CoordTransformObject;
import ValkyrienWarfareBase.PhysicsManagement.PhysicsWrapperEntity;
import ValkyrienWarfareCombat.Entity.EntityCannonBall;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class EntityDraggable {

	public PhysicsWrapperEntity worldBelowFeet;

	public Vector velocityAddedToPlayer = new Vector();
	public Entity draggableAsEntity;
	public double yawDifVelocity;

	public EntityDraggable(){
		draggableAsEntity = getEntityFromDraggable(this);
	}

	public static void tickAddedVelocityForWorld(World world){
		try{
			for(int i = 0;i < world.loadedEntityList.size(); i++){
				Entity e = world.loadedEntityList.get(i);
				//TODO: Maybe add a check to prevent moving entities that are fixed onto a Ship, but I like the visual effect
				if(!(e instanceof PhysicsWrapperEntity)&&!(e instanceof EntityCannonBall)){
					EntityDraggable draggable = getDraggableFromEntity(e);
					draggable.tickAddedVelocity();

					if(draggable.worldBelowFeet == null){
						if(draggable.draggableAsEntity.onGround){
							draggable.velocityAddedToPlayer.zero();
							draggable.yawDifVelocity = 0;
						}else{
							if(draggable.draggableAsEntity instanceof EntityPlayer){
								EntityPlayer player = (EntityPlayer) draggable.draggableAsEntity;
								if(player.isCreative() && player.capabilities.isFlying){
									draggable.velocityAddedToPlayer.multiply(.99D * .95D);
									draggable.yawDifVelocity *= .95D * .95D;
								}
							}
						}
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	//TODO: Finishme
	public void tickAddedVelocity(){
		if (worldBelowFeet != null && !ValkyrienWarfareMod.physicsManager.isEntityFixed(draggableAsEntity)) {
			CoordTransformObject coordTransform = worldBelowFeet.wrapping.coordTransform;

			float rotYaw = draggableAsEntity.rotationYaw;
			float rotPitch = draggableAsEntity.rotationPitch;
			float prevYaw = draggableAsEntity.prevRotationYaw;
			float prevPitch = draggableAsEntity.prevRotationPitch;

			Vector oldPos = new Vector(draggableAsEntity);

			RotationMatrices.applyTransform(coordTransform.prevwToLTransform, coordTransform.prevWToLRotation, draggableAsEntity);
			RotationMatrices.applyTransform(coordTransform.lToWTransform, coordTransform.lToWRotation, draggableAsEntity);

			Vector newPos = new Vector(draggableAsEntity);

			//Move the entity back to its old position, the added velocity will be used afterwards
			draggableAsEntity.setPosition(oldPos.X, oldPos.Y, oldPos.Z);
			Vector addedVel = oldPos.getSubtraction(newPos);

			velocityAddedToPlayer = addedVel;

			draggableAsEntity.rotationYaw = rotYaw;
			draggableAsEntity.rotationPitch = rotPitch;
			draggableAsEntity.prevRotationYaw = prevYaw;
			draggableAsEntity.prevRotationPitch = prevPitch;

			Vector oldLookingPos = new Vector(draggableAsEntity.getLook(1.0F));
			RotationMatrices.applyTransform(coordTransform.prevWToLRotation, oldLookingPos);
			RotationMatrices.applyTransform(coordTransform.lToWRotation, oldLookingPos);

			double newPitch = Math.asin(oldLookingPos.Y) * -180D / Math.PI;
			double f4 = -Math.cos(-newPitch * 0.017453292D);
			double radianYaw = Math.atan2((oldLookingPos.X / f4), (oldLookingPos.Z / f4));
			radianYaw += Math.PI;
			radianYaw *= -180D / Math.PI;


			if (!(Double.isNaN(radianYaw) || Math.abs(newPitch) > 85)) {
				double wrappedYaw = MathHelper.wrapDegrees(radianYaw);
				double wrappedRotYaw = MathHelper.wrapDegrees(draggableAsEntity.rotationYaw);
				double yawDif = wrappedYaw - wrappedRotYaw;
				if (Math.abs(yawDif) > 180D) {
					if (yawDif < 0) {
						yawDif += 360D;
					} else {
						yawDif -= 360D;
					}
				}
				yawDif %= 360D;
				final double threshold = .1D;
				if (Math.abs(yawDif) < threshold) {
					yawDif = 0D;
				}
				yawDifVelocity = yawDif;
			}
		}

		boolean onGroundOrig = draggableAsEntity.onGround;

		if(!ValkyrienWarfareMod.physicsManager.isEntityFixed(draggableAsEntity)){
			float originalWalked = draggableAsEntity.distanceWalkedModified;
			float originalWalkedOnStep = draggableAsEntity.distanceWalkedOnStepModified;
			boolean originallySneaking = draggableAsEntity.isSneaking();

			draggableAsEntity.setSneaking(false);

			if(draggableAsEntity.worldObj.isRemote && draggableAsEntity instanceof EntityPlayerSP){
				EntityPlayerSP playerSP = (EntityPlayerSP)draggableAsEntity;
				MovementInput moveInput = playerSP.movementInput;
				originallySneaking = moveInput.sneak;
				moveInput.sneak = false;
			}

			draggableAsEntity.moveEntity(velocityAddedToPlayer.X, velocityAddedToPlayer.Y, velocityAddedToPlayer.Z);
//			CallRunner.onEntityMove(draggableAsEntity, velocityAddedToPlayer.X, velocityAddedToPlayer.Y, velocityAddedToPlayer.Z);

			if (!(draggableAsEntity instanceof EntityPlayer)) {
				if (draggableAsEntity instanceof EntityArrow) {
					draggableAsEntity.prevRotationYaw = draggableAsEntity.rotationYaw;
					draggableAsEntity.rotationYaw -= yawDifVelocity;
				} else {
					draggableAsEntity.prevRotationYaw = draggableAsEntity.rotationYaw;
					draggableAsEntity.rotationYaw += yawDifVelocity;
				}
			} else {
				if (draggableAsEntity.worldObj.isRemote) {
					draggableAsEntity.prevRotationYaw = draggableAsEntity.rotationYaw;
					draggableAsEntity.rotationYaw += yawDifVelocity;
				}
			}

			//Do not add this movement as if the entity were walking it
			draggableAsEntity.distanceWalkedModified = originalWalked;
			draggableAsEntity.distanceWalkedOnStepModified = originalWalkedOnStep;
			draggableAsEntity.setSneaking(originallySneaking);

			if(draggableAsEntity.worldObj.isRemote && draggableAsEntity instanceof EntityPlayerSP){
				EntityPlayerSP playerSP = (EntityPlayerSP)draggableAsEntity;
				MovementInput moveInput = playerSP.movementInput;
				moveInput.sneak = originallySneaking;
			}
		}

		if(onGroundOrig){
			draggableAsEntity.onGround = onGroundOrig;
		}

		velocityAddedToPlayer.multiply(.99D);
		yawDifVelocity *= .95D;
	}

	public static EntityDraggable getDraggableFromEntity(Entity entity){
		if(entity == null){
			return null;
		}
		Object o = entity;
		return (EntityDraggable)o;
	}

	public static Entity getEntityFromDraggable(EntityDraggable draggable){
		if(draggable == null){
			return null;
		}
		Object o = draggable;
		return (Entity)o;
	}

}
