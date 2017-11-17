package openflextrack.api;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * An interface that allows custom sleeper types.
 * 
 * @author Leshuwa Kaiheiwa
 */
public interface ISleeperType {
	
	/**
	 * Return the distance between sleepers (in meters).<br>
	 * <br>
	 * <i>Offset is also directly related to rail smoothness;
	 * smaller offset results in closer ties and smoother rails.</i>
	 */
	float getOffset();

	/**
	 * Return a texture for the sleepers.
	 */
	@SideOnly(Side.CLIENT)
	ResourceLocation getTexture();
	
	/**
	 * Render a single sleeper with given width.
	 * 
	 * @param width - The sleeper's width (in meters).
	 */
	@SideOnly(Side.CLIENT)
	void render(float width);
}