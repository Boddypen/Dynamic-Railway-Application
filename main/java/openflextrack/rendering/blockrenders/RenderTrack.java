package openflextrack.rendering.blockrenders;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import openflextrack.OFT;
import openflextrack.baseclasses.OFTCurve;
import openflextrack.blocks.TileEntityTrack;
import openflextrack.rendering.blockmodels.ModelTrackTie;

public class RenderTrack extends TileEntitySpecialRenderer{
	private static final ModelTrackTie modelTie = new ModelTrackTie();
	private static final ResourceLocation tieTexture = new ResourceLocation(OFT.MODID, "textures/blockmodels/tie.png");
	private static final ResourceLocation railTexture = new ResourceLocation(OFT.MODID, "textures/blockmodels/rail.png");
	private static final ResourceLocation ballastTexture = new ResourceLocation(OFT.MODID, "textures/blocks/ballast.png");

	//These define what the rails look like.  Change ONLY if rail shapes need to change.
	private static final float bottomInnerX = 11.5F/16F;
	private static final float bottomOuterX = 16.5F/16F;
	private static final float bottomLowerY = 0F/16F;
	private static final float bottomUpperY = 1F/16F;
	
	private static final float middleInnerX = 13.5F/16F;
	private static final float middleOuterX = 14.5F/16F;
	
	private static final float upperInnerX = 13F/16F;
	private static final float upperOuterX = 15F/16F;
	private static final float upperLowerY = 3F/16F;
	private static final float upperUpperY = 4F/16F;
	
	public RenderTrack(){}
	
	@Override
	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks, int destroyStage){
		super.renderTileEntityAt(tile, x, y, z, partialTicks, destroyStage);
		TileEntityTrack track = (TileEntityTrack) tile;
		if(track.curve != null){
			TileEntity trackTileEntity = track.getWorld().getTileEntity(track.getPos().add(track.curve.endPos));
			if(!(trackTileEntity instanceof TileEntityTrack)){
				//Sometimes the TE's don't break evenly.  Make sure this doesn't happen and we try to render a flag here.
				return;
			}
			TileEntityTrack otherEnd = (TileEntityTrack) trackTileEntity;
			
			//Quick check to see if connection is still valid.
			if(track.connectedTrack != null){
				if(track.connectedTrack.isInvalid()){
					track.connectedTrack = null;
					track.hasTriedToConnectToOtherSegment = false;
				}
			}
			
			//If this Tile Entity is not connected, and has not tried to connect, do so now.
			if(!track.hasTriedToConnectToOtherSegment){
				track.hasTriedToConnectToOtherSegment = connectToAdjacentTracks(track);
			}
			
			//Make sure not to render if the other end has done so.
			if(otherEnd != null){
				//Try to keep the same track rendering if possible.
				//If the track isn't null, render that one instead.
				boolean renderFromOtherEnd = otherEnd.getPos().getX() != track.getPos().getX() ? otherEnd.getPos().getX() > track.getPos().getX() : otherEnd.getPos().getZ() > track.getPos().getZ(); 
				if(renderFromOtherEnd){
					this.renderTileEntityAt(otherEnd, x + otherEnd.getPos().getX() - track.getPos().getX(), y + otherEnd.getPos().getY() - track.getPos().getY(), z + otherEnd.getPos().getZ() - track.getPos().getZ(), partialTicks, destroyStage);
					return;
				}
			}
			
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			GL11.glPushMatrix();
			renderTrackSegmentFromCurve(track.getWorld(), track.getPos(), track.curve, false, track.connectedTrack, otherEnd != null ? otherEnd.connectedTrack : null);
			GL11.glPopMatrix();
			
			
			//CAUSES OVER 20FPS LOSS.  DO NOT USE EXCEPT FOR TESTING!
			/*
			Minecraft.getMinecraft().getTextureManager().bindTexture(ballastTexture);
			//Render master tracks with ballast.
			GL11.glPushMatrix();
			int lightValue = track.getWorld().getCombinedLight(track.curve.blockStartPos, 0);
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightValue%65536, lightValue/65536);
			drawBallastBox(1F/16F);
			
			GL11.glTranslatef(track.curve.blockEndPos.getX() - track.curve.blockStartPos.getX(), track.curve.blockEndPos.getY() - track.curve.blockStartPos.getY(), track.curve.blockEndPos.getZ() - track.curve.blockStartPos.getZ());
			lightValue = track.getWorld().getCombinedLight(track.curve.blockEndPos, 0);
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightValue%65536, lightValue/65536);
			drawBallastBox(1F/16F);
			GL11.glPopMatrix();
			
			//Render fake tracks with ballast.
			for(BlockPos fakePos : track.getFakeTracks()){
				//Sometimes while breaking states don't update at the same time.
				//Make sure we have a fake track here.
				if(track.getWorld().getBlockState(fakePos).getBlock().equals(MTSRegistry.trackStructureFake)){
					GL11.glPushMatrix();
					GL11.glTranslatef(fakePos.getX() - track.curve.blockStartPos.getX(), fakePos.getY() - track.curve.blockStartPos.getY(), fakePos.getZ() - track.curve.blockStartPos.getZ());
					lightValue = track.getWorld().getCombinedLight(fakePos, 0);
					OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightValue%65536, lightValue/65536);
					drawBallastBox(track.getWorld().getBlockState(fakePos).getValue(BlockTrackStructureFake.height)/16F);
					GL11.glPopMatrix();
				}
			}*/
			GL11.glPopMatrix();
		}
	}
	
	private static boolean connectToAdjacentTracks(TileEntityTrack track){
		for(byte i=-1; i<=1; ++i){
			for(byte j=-1; j<=1; ++j){
				if(!(i == 0 && j == 0)){
					TileEntity testTile = track.getWorld().getTileEntity(track.getPos().add(i, 0, j));
					if(testTile instanceof TileEntityTrack){
						if(((TileEntityTrack) testTile).curve != null){
							if(((TileEntityTrack) testTile).curve.startAngle == (180 + track.curve.startAngle)%360){
								//Make sure we don't link to ourselves.  Because players will find a way to make this happen.
								if(!testTile.getPos().equals(track.getPos().add(track.curve.endPos))){
									//If the track we want to link to has already linked with us, stop the link.
									//Double linkings cause double rendering and lots of errors.
									if(!track.equals(((TileEntityTrack) testTile).connectedTrack)){
										track.connectedTrack = (TileEntityTrack) testTile;
									}
									return true;
								}
							}
						}else{
							//Wait another tick for the curve of the found track to init.
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * This can be called to render track anywhere in the code, not just from this class.
	 */
	public static void renderTrackSegmentFromCurve(World world, BlockPos startPos, OFTCurve curve, boolean holographic, TileEntityTrack startConnector, TileEntityTrack endConnector){
		final float offset = 0.65F;
		float textureOffset = 0;
		List<float[]> texPoints = new ArrayList<float[]>();
		float[] currentPoint;
		float currentAngle;
		
		//First get information about what connectors need rendering.
		boolean renderStartTie = false;
		boolean renderStartRail = false;
		boolean renderStartRailExtra = false;
		TileEntityTrack startConnectorMaster = null;
		if(startConnector != null){
			if(startConnector.curve != null){	
				startConnectorMaster = (TileEntityTrack) world.getTileEntity(startConnector.getPos().add(startConnector.curve.endPos));
				if(startConnectorMaster != null){
					boolean renderFromOtherEnd = startConnectorMaster.getPos().getX() != startConnector.getPos().getX() ? startConnectorMaster.getPos().getX() > startConnector.getPos().getX() : startConnectorMaster.getPos().getZ() > startConnector.getPos().getZ();
					if(renderFromOtherEnd){
						//Start connecter is the end of a rail.  Test for tie space.
						if(startConnectorMaster.curve != null){
							renderStartRailExtra = true;
							if(startConnectorMaster.curve.pathLength%offset > offset/2){
								renderStartTie = true;
							}
						}
					}
				}else{
					renderStartRail = true;
				}
			}
		}
		
		boolean renderEndTie = false;
		boolean renderEndRail = false;
		boolean renderEndRailExtra = false;
		TileEntityTrack endConnectorMaster = null;
		if(endConnector != null){
			if(endConnector.curve != null){
				endConnectorMaster = (TileEntityTrack) world.getTileEntity(endConnector.getPos().add(endConnector.curve.endPos));
				if(endConnectorMaster != null){
					boolean renderFromOtherEnd = endConnectorMaster.getPos().getX() != endConnector.getPos().getX() ? endConnectorMaster.getPos().getX() > endConnector.getPos().getX() : endConnectorMaster.getPos().getZ() > endConnector.getPos().getZ();
					if(renderFromOtherEnd){
						//End connecter is the end of a rail.  Test for tie space.
						if(endConnectorMaster.curve != null){
							renderEndRailExtra = true;
							if(endConnectorMaster.curve.pathLength%offset + curve.pathLength%offset > offset/2){
								renderEndTie = true;
							}
						}
					}
				}else{
					renderEndRail = true;
				}
			}
		}	
		
		//Get an extra start rail segment if needed.
		if(renderStartRailExtra){
			//Get the remainder of what rails have not been rendered and add that point.
			float lastPointOnCurve = (startConnectorMaster.curve.pathLength - (startConnectorMaster.curve.pathLength%offset))/startConnectorMaster.curve.pathLength;
			currentPoint = startConnectorMaster.curve.getCachedPointAt(lastPointOnCurve);
			currentAngle = startConnectorMaster.curve.getCachedYawAngleAt(lastPointOnCurve);
			textureOffset = (float) -(Math.hypot(currentPoint[0] - startConnector.getPos().getX(), currentPoint[2] - startConnector.getPos().getZ()) + Math.hypot(startConnector.getPos().getX(), startConnector.getPos().getZ()));
			texPoints.add(new float[]{
				currentPoint[0],
				currentPoint[1] + 0.1875F,
				currentPoint[2],
				(float) Math.sin(Math.toRadians(currentAngle)),
				(float) Math.cos(Math.toRadians(currentAngle)),
				(float) (textureOffset),
				world.getCombinedLight(new BlockPos((int) Math.ceil(currentPoint[0]), (int) Math.ceil(currentPoint[1]), (int) Math.ceil(currentPoint[2])).add(startPos), 0)
			});
		}
		
		//Get a start tie if needed.
		if(renderStartTie || (renderStartRail && !renderStartRailExtra)){
			currentPoint = startConnector.curve.getCachedPointAt(0);
			currentAngle = (startConnector.curve.getCachedYawAngleAt(0) +180)%360;
			textureOffset = (float) -Math.hypot(currentPoint[0], currentPoint[2]);
			texPoints.add(new float[]{
				currentPoint[0],
				currentPoint[1] + 0.1875F,
				currentPoint[2],
				(float) Math.sin(Math.toRadians(currentAngle)),
				(float) Math.cos(Math.toRadians(currentAngle)),
				(float) (textureOffset),
				world.getCombinedLight(new BlockPos((int) Math.ceil(currentPoint[0]), (int) Math.ceil(currentPoint[1]), (int) Math.ceil(currentPoint[2])).add(startPos), 0)
			});
		}

		//Get the regular ties and rails.
		for(float f=0; f <= curve.pathLength; f += offset){
			currentPoint = curve.getCachedPointAt(f/curve.pathLength);
			currentAngle = curve.getCachedYawAngleAt(f/curve.pathLength);
			if(f != 0){
				textureOffset += (float) Math.hypot(currentPoint[0] - texPoints.get(texPoints.size() - 1)[0], currentPoint[2] - texPoints.get(texPoints.size() - 1)[2]);
			}else{
				textureOffset = 0;
			}
			texPoints.add(new float[]{
				currentPoint[0],
				currentPoint[1] + 0.1875F,
				currentPoint[2],
				(float) Math.sin(Math.toRadians(currentAngle)),
				(float) Math.cos(Math.toRadians(currentAngle)),
				(float) (textureOffset),
                world.getCombinedLight(new BlockPos((int) Math.ceil(currentPoint[0]), (int) Math.ceil(currentPoint[1]), (int) Math.ceil(currentPoint[2])).add(startPos), 0)
			});
		}
		
		//Get an end tie if needed.
		if(renderEndTie || (renderEndRail && !renderEndRailExtra)){
			currentPoint = endConnector.curve.getCachedPointAt(0);
			currentAngle = endConnector.curve.startAngle;
			textureOffset += (float) Math.hypot(currentPoint[0] - texPoints.get(texPoints.size() - 1)[0], currentPoint[2] - texPoints.get(texPoints.size() - 1)[2]);
			texPoints.add(new float[]{
				currentPoint[0],
				currentPoint[1] + 0.1875F,
				currentPoint[2],
				(float) Math.sin(Math.toRadians(currentAngle)),
				(float) Math.cos(Math.toRadians(currentAngle)),
				(float) (textureOffset),
				world.getCombinedLight(new BlockPos((int) Math.ceil(currentPoint[0]), (int) Math.ceil(currentPoint[1]), (int) Math.ceil(currentPoint[2])).add(startPos), 0)
			});			
		}
		
		//Get an extra end rail segment if needed.
		if(renderEndRailExtra){
			//Get the remainder of what rails have not been rendered and add that point.
			float lastPointOnCurve = (endConnectorMaster.curve.pathLength - (endConnectorMaster.curve.pathLength%offset))/endConnectorMaster.curve.pathLength;
			currentPoint = endConnectorMaster.curve.getCachedPointAt(lastPointOnCurve);
			currentAngle = (endConnectorMaster.curve.getCachedYawAngleAt(lastPointOnCurve) + 180)%360;
			textureOffset += (float) Math.hypot(currentPoint[0] - texPoints.get(texPoints.size() - 1)[0], currentPoint[2] - texPoints.get(texPoints.size() - 1)[2]);
			texPoints.add(new float[]{
				currentPoint[0],
				currentPoint[1] + 0.1875F,
				currentPoint[2],
				(float) Math.sin(Math.toRadians(currentAngle)),
				(float) Math.cos(Math.toRadians(currentAngle)),
				(float) (textureOffset),
				world.getCombinedLight(new BlockPos((int) Math.ceil(currentPoint[0]), (int) Math.ceil(currentPoint[1]), (int) Math.ceil(currentPoint[2])).add(startPos), 0)
			});
		}
		
		//Now that we have all the points, it's time to render.
		if(holographic){
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glColor4f(0, 1, 0, 0.25F);
		}

		//First, render any connector ties as they cause trouble in the main loop.
		if(renderStartTie){
			GL11.glPushMatrix();
			GL11.glTranslatef(texPoints.get(1)[0], texPoints.get(1)[1] - 0.1875F, texPoints.get(1)[2]);
			GL11.glRotatef(-curve.startAngle, 0, 1, 0);
			renderTie(texPoints.get(1)[6], holographic);
			GL11.glPopMatrix();
		}
		
		if(renderEndTie){
			GL11.glPushMatrix();
			GL11.glTranslatef(texPoints.get(texPoints.size() - 2)[0], texPoints.get(texPoints.size() - 2)[1] - 0.1875F, texPoints.get(texPoints.size() - 2)[2]);
			GL11.glRotatef(-endConnector.curve.startAngle, 0, 1, 0);
			renderTie(texPoints.get(texPoints.size() - 2)[6], holographic);
			GL11.glPopMatrix();
		}
		
		//Now render the ties, making sure to avoid the start and end connectors.
		byte startIndex = (byte) (renderStartTie ? 2 : (renderStartRail || renderStartRailExtra ? 1 : 0));
		for(short i = startIndex; i < texPoints.size() - (renderEndTie ? 2 : ((renderEndRail || renderEndRailExtra) ? 1 : 0)); ++i){
			GL11.glPushMatrix();
			GL11.glTranslatef(texPoints.get(i)[0], texPoints.get(i)[1] - 0.1875F, texPoints.get(i)[2]);
			GL11.glRotatef(-curve.getCachedYawAngleAt((i - startIndex)*offset/curve.pathLength), 0, 1, 0);
			GL11.glRotatef(curve.getCachedPitchAngleAt((i - startIndex)*offset/curve.pathLength), 1, 0, 0);
			renderTie(texPoints.get(i)[6], holographic);
			GL11.glPopMatrix();
		}
		
		//Now to render the rails.
		//These use all the points so no special logic is required.		
		GL11.glPushMatrix();
		Minecraft.getMinecraft().getTextureManager().bindTexture(railTexture);
		drawRailSegment(texPoints, bottomInnerX, bottomOuterX, bottomLowerY, bottomLowerY, 0.0F, 3F/19F, holographic);//Bottom
		drawRailSegment(texPoints, bottomOuterX, bottomOuterX, bottomLowerY, bottomUpperY, 3F/19F, 4F/19F, holographic);//Outer-bottom-side
		drawRailSegment(texPoints, bottomOuterX, middleOuterX, bottomUpperY, bottomUpperY, 4F/19F, 5.5F/19F, holographic);//Outer-bottom-top
		drawRailSegment(texPoints, middleOuterX, middleOuterX, bottomUpperY, upperLowerY, 6F/19F, 8F/19F, holographic);//Outer-middle
		drawRailSegment(texPoints, middleOuterX, upperOuterX, upperLowerY, upperLowerY, 8F/19F, 8.5F/19F, holographic);//Outer-top-under
		drawRailSegment(texPoints, upperOuterX, upperOuterX, upperLowerY, upperUpperY, 9F/19F, 10F/19F, holographic);//Outer-top-side
		drawRailSegment(texPoints, upperOuterX, upperInnerX, upperUpperY, upperUpperY, 10F/19F, 12F/19F, holographic);//Top
		drawRailSegment(texPoints, upperInnerX, upperInnerX, upperUpperY, upperLowerY, 12F/19F, 13F/19F, holographic);//Inner-top-side
		drawRailSegment(texPoints, upperInnerX, middleInnerX, upperLowerY, upperLowerY, 13F/19F, 13.5F/19F, holographic);//Inner-top-under
		drawRailSegment(texPoints, middleInnerX, middleInnerX, upperLowerY, bottomUpperY, 14F/19F, 16F/19F, holographic);//Inner-middle
		drawRailSegment(texPoints, middleInnerX, bottomInnerX, bottomUpperY, bottomUpperY, 16F/19F, 17.5F/19F, holographic);//Inner-bottom-top
		drawRailSegment(texPoints, bottomInnerX, bottomInnerX, bottomUpperY, bottomLowerY, 18F/19F, 19F/19F, holographic);//Inner-bottom-side
		GL11.glPopMatrix();
		
		//Finally, render a end cap on rails if there's no connection.
		//Note that if another rail connects with this one, the cap will still be rendered.
		//Not a big deal in the grand scheme of things.
		
		if(!renderStartRail){
			GL11.glPushMatrix();
			GL11.glTranslatef(texPoints.get(0)[0], texPoints.get(0)[1], texPoints.get(0)[2]);
			drawRailEndCaps(texPoints.get(0), holographic);
			GL11.glPopMatrix();
		}
		if(!renderEndRail){
			GL11.glPushMatrix();
			GL11.glTranslatef(texPoints.get(texPoints.size() - 1)[0], texPoints.get(texPoints.size() - 1)[1], texPoints.get(texPoints.size() - 1)[2]);
			GL11.glRotatef(180, 0, 1, 0);
			drawRailEndCaps(texPoints.get(texPoints.size() - 1), holographic);
			GL11.glPopMatrix();
		}
		if(holographic){
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glColor4f(1, 1, 1, 1);
		}
	}
	
	private static void renderTie(float brightness, boolean holographic){
		if(!holographic){
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightness%65536, brightness/65536);
		}
		Minecraft.getMinecraft().getTextureManager().bindTexture(tieTexture);
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glTranslatef(0, 0, -0.1875F);
		modelTie.render();
	}
	
	private static void drawRailSegment(List<float[]> texPoints, float w1, float w2, float h1, float h2, float t1, float t2, boolean holographic){
		GL11.glBegin(GL11.GL_QUAD_STRIP);
		for(float[] point : texPoints){
			if(!holographic)OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, point[6]%65536, point[6]/65536);
			GL11.glTexCoord2d(point[5], t2);
			GL11.glNormal3f(0, 1, 0);
			GL11.glVertex3d(point[0] + w1*point[4], point[1] + h1, point[2] + w1*point[3]);
			GL11.glTexCoord2d(point[5], t1);
			GL11.glNormal3f(0, 1, 0);
			GL11.glVertex3d(point[0] + w2*point[4], point[1] + h2, point[2] + w2*point[3]);
		}
		GL11.glEnd();
		GL11.glBegin(GL11.GL_QUAD_STRIP);
		for(float[] point : texPoints){
			if(!holographic)OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, point[6]%65536, point[6]/65536);
            GL11.glTexCoord2d(point[5], t1);
            GL11.glNormal3f(0, 1, 0);
			GL11.glVertex3d(point[0] - w2*point[4], point[1] + h2, point[2] - w2*point[3]);
			GL11.glTexCoord2d(point[5], t2);
			GL11.glNormal3f(0, 1, 0);
			GL11.glVertex3d(point[0] - w1*point[4], point[1] + h1, point[2] - w1*point[3]);
		}
		GL11.glEnd();
	}
	
	private static void drawRailEndCaps(float[] texPoint, boolean holographic){		
		GL11.glPushMatrix();
		GL11.glBegin(GL11.GL_QUADS);
			if(!holographic)OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, texPoint[6]%65536, texPoint[6]/65536);
			GL11.glTexCoord2d(0F/19F, 6F/19F);
			GL11.glVertex3d(bottomOuterX*texPoint[4], bottomLowerY, bottomOuterX*texPoint[3]);
			GL11.glTexCoord2d(5F/19F, 6F/19F);
			GL11.glVertex3d(bottomInnerX*texPoint[4], bottomLowerY, bottomInnerX*texPoint[3]);
			GL11.glTexCoord2d(5F/19F, 7F/19F);
			GL11.glVertex3d(bottomInnerX*texPoint[4], bottomUpperY, bottomInnerX*texPoint[3]);
			GL11.glTexCoord2d(0F/19F, 7F/19F);
			GL11.glVertex3d(bottomOuterX*texPoint[4], bottomUpperY, bottomOuterX*texPoint[3]);
			
			GL11.glTexCoord2d(0F/19F, 7F/19F);
			GL11.glVertex3d(middleOuterX*texPoint[4], bottomUpperY, middleOuterX*texPoint[3]);
			GL11.glTexCoord2d(1F/19F, 7F/19F);
			GL11.glVertex3d(middleInnerX*texPoint[4], bottomUpperY, middleInnerX*texPoint[3]);
			GL11.glTexCoord2d(1F/19F, 9F/19F);
			GL11.glVertex3d(middleInnerX*texPoint[4], upperLowerY, middleInnerX*texPoint[3]);
			GL11.glTexCoord2d(0F/19F, 9F/19F);
			GL11.glVertex3d(middleOuterX*texPoint[4], upperLowerY, middleOuterX*texPoint[3]);
			
			GL11.glTexCoord2d(0F/19F, 9F/19F);
			GL11.glVertex3d(upperOuterX*texPoint[4], upperLowerY, upperOuterX*texPoint[3]);
			GL11.glTexCoord2d(2F/19F, 9F/19F);
			GL11.glVertex3d(upperInnerX*texPoint[4], upperLowerY, upperInnerX*texPoint[3]);
			GL11.glTexCoord2d(2F/19F, 10F/19F);
			GL11.glVertex3d(upperInnerX*texPoint[4], upperUpperY, upperInnerX*texPoint[3]);
			GL11.glTexCoord2d(0F/19F, 10F/19F);
			GL11.glVertex3d(upperOuterX*texPoint[4], upperUpperY, upperOuterX*texPoint[3]);

			GL11.glTexCoord2d(5F/19F, 6F/19F);
			GL11.glVertex3d(-bottomInnerX*texPoint[4], bottomLowerY, -bottomInnerX*texPoint[3]);
			GL11.glTexCoord2d(0F/19F, 6F/19F);
			GL11.glVertex3d(-bottomOuterX*texPoint[4], bottomLowerY, -bottomOuterX*texPoint[3]);
			GL11.glTexCoord2d(0F/19F, 7F/19F);
			GL11.glVertex3d(-bottomOuterX*texPoint[4], bottomUpperY, -bottomOuterX*texPoint[3]);
			GL11.glTexCoord2d(5F/19F, 7F/19F);
			GL11.glVertex3d(-bottomInnerX*texPoint[4], bottomUpperY, -bottomInnerX*texPoint[3]);
			
			GL11.glTexCoord2d(1F/19F, 7F/19F);
			GL11.glVertex3d(-middleInnerX*texPoint[4], bottomUpperY, -middleInnerX*texPoint[3]);
			GL11.glTexCoord2d(0F/19F, 7F/19F);
			GL11.glVertex3d(-middleOuterX*texPoint[4], bottomUpperY, -middleOuterX*texPoint[3]);
			GL11.glTexCoord2d(0F/19F, 9F/19F);
			GL11.glVertex3d(-middleOuterX*texPoint[4], upperLowerY, -middleOuterX*texPoint[3]);
			GL11.glTexCoord2d(1F/19F, 9F/19F);
			GL11.glVertex3d(-middleInnerX*texPoint[4], upperLowerY, -middleInnerX*texPoint[3]);
			
			GL11.glTexCoord2d(2F/19F, 9F/19F);
			GL11.glVertex3d(-upperInnerX*texPoint[4], upperLowerY, -upperInnerX*texPoint[3]);
			GL11.glTexCoord2d(0F/19F, 9F/19F);
			GL11.glVertex3d(-upperOuterX*texPoint[4], upperLowerY, -upperOuterX*texPoint[3]);
			GL11.glTexCoord2d(0F/19F, 10F/19F);
			GL11.glVertex3d(-upperOuterX*texPoint[4], upperUpperY, -upperOuterX*texPoint[3]);
			GL11.glTexCoord2d(2F/19F, 10F/19F);
			GL11.glVertex3d(-upperInnerX*texPoint[4], upperUpperY, -upperInnerX*texPoint[3]);
		GL11.glEnd();
		GL11.glPopMatrix();
	}
	
	private static void drawBallastBox(float height){
		//TODO edit the fake tracks to make these go in to a Json def.
		height += 1/16F;
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(1, 1 - height);
		GL11.glNormal3f(-1, 0, 0);
		GL11.glVertex3d(0, height, 0);
		GL11.glTexCoord2f(1, 1);
		GL11.glNormal3f(-1, 0, 0);
		GL11.glVertex3d(0, -0.01, 0);
		GL11.glTexCoord2f(0, 1);
		GL11.glNormal3f(-1, 0, 0);
		GL11.glVertex3d(0, -0.01, 1);
		GL11.glTexCoord2f(0, 1 - height);
		GL11.glNormal3f(-1, 0, 0);
		GL11.glVertex3d(0, height, 1);
		
		GL11.glTexCoord2f(0, 1 - height);
		GL11.glNormal3f(1, 0, 0);
		GL11.glVertex3d(1, height, 1);
		GL11.glTexCoord2f(0, 1);
		GL11.glNormal3f(1, 0, 0);
		GL11.glVertex3d(1, -0.01, 1);
		GL11.glTexCoord2f(1, 1);
		GL11.glNormal3f(1, 0, 0);
		GL11.glVertex3d(1, -0.01, 0);
		GL11.glTexCoord2f(1, 1 - height);
		GL11.glNormal3f(1, 0, 0);
		GL11.glVertex3d(1, height, 0);
		
		GL11.glTexCoord2f(0, 1 - height);
		GL11.glNormal3f(0, 0, -1);
		GL11.glVertex3d(1, height, 0);
		GL11.glTexCoord2f(0, 1);
		GL11.glNormal3f(0, 0, -1);
		GL11.glVertex3d(1, -0.01, 0);
		GL11.glTexCoord2f(1, 1);
		GL11.glNormal3f(0, 0, -1);
		GL11.glVertex3d(0, -0.01, 0);
		GL11.glTexCoord2f(1, 1 - height);
		GL11.glNormal3f(0, 0, -1);
		GL11.glVertex3d(0, height, 0);
		
		GL11.glTexCoord2f(1, 1 - height);
		GL11.glNormal3f(0, 0, 1);
		GL11.glVertex3d(0, height, 1);
		GL11.glTexCoord2f(1, 1);
		GL11.glNormal3f(0, 0, 1);
		GL11.glVertex3d(0, -0.01, 1);
		GL11.glTexCoord2f(0, 1);
		GL11.glNormal3f(0, 0, 1);
		GL11.glVertex3d(1, -0.01, 1);
		GL11.glTexCoord2f(0, 1 - height);
		GL11.glNormal3f(0, 0, 1);
		GL11.glVertex3d(1, height, 1);
		
		GL11.glTexCoord2f(1, 0);
		GL11.glNormal3f(0, 1, 0);
		GL11.glVertex3d(1, height, 1);
		GL11.glTexCoord2f(1, 1);
		GL11.glNormal3f(0, 1, 0);
		GL11.glVertex3d(1, height, 0);
		GL11.glTexCoord2f(0, 1);
		GL11.glNormal3f(0, 1, 0);
		GL11.glVertex3d(0, height, 0);
		GL11.glTexCoord2f(0, 0);
		GL11.glNormal3f(0, 1, 0);
		GL11.glVertex3d(0, height, 1);
		
		GL11.glTexCoord2f(0, 0);
		GL11.glNormal3f(0, -1, 0);
		GL11.glVertex3d(0, -0.01, 1);
		GL11.glTexCoord2f(0, 1);
		GL11.glNormal3f(0, -1, 0);
		GL11.glVertex3d(0, -0.01, 0);
		GL11.glTexCoord2f(1, 1);
		GL11.glNormal3f(0, -1, 0);
		GL11.glVertex3d(1, -0.01, 0);
		GL11.glTexCoord2f(1, 0);
		GL11.glNormal3f(0, -1, 0);
		GL11.glVertex3d(1, -0.01, 1);
		GL11.glEnd();
	}
}