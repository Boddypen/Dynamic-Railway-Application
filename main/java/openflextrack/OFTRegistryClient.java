package openflextrack;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import openflextrack.blocks.TileEntitySurveyFlag;
import openflextrack.blocks.TileEntityTrackStructure;
import openflextrack.rendering.blockrenders.RenderSurveyFlag;
import openflextrack.rendering.blockrenders.RenderTrack;

public class OFTRegistryClient{
	private static final OFTRegistryClient instance = new OFTRegistryClient();
	/**Map of parsed models keyed by name.*/
	public static final Map<String, Map<String, Float[][]>> modelMap = new HashMap<String, Map<String, Float[][]>>();

	public static void preInit(){
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySurveyFlag.class, new RenderSurveyFlag());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTrackStructure.class, new RenderTrack());
	}
	
	public static void init(){
		ItemModelMesher mesher = Minecraft.getMinecraft().getRenderItem().getItemModelMesher();
		mesher.register(OFTRegistry.ties, 0, new ModelResourceLocation(OFT.MODID + ":" + "ties", "inventory"));
		mesher.register(OFTRegistry.rails, 0, new ModelResourceLocation(OFT.MODID + ":" + "rails", "inventory"));
		mesher.register(OFTRegistry.track, 0, new ModelResourceLocation(OFT.MODID + ":" + "track", "inventory"));
		mesher.register(Item.getItemFromBlock(OFTRegistry.surveyFlag), 0, new ModelResourceLocation(OFT.MODID + ":" + "surveyflag", "inventory"));
	}
}
