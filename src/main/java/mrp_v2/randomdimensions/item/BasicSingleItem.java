package mrp_v2.randomdimensions.item;

import mrp_v2.randomdimensions.RandomDimensions;
import mrp_v2.randomdimensions.util.ObjectHolder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Function;

public class BasicSingleItem extends Item
{
    public static final ItemGroup MAIN_ITEM_GROUP = new ItemGroup(RandomDimensions.ID)
    {
        @Override @OnlyIn(Dist.CLIENT) public ItemStack createIcon()
        {
            return new ItemStack(ObjectHolder.PORTAL_CONTROLLER_BLOCK_ITEM);
        }
    };

    public BasicSingleItem(String id)
    {
        this((properties) -> properties, id);
    }

    public BasicSingleItem(Function<Properties, Properties> propertiesModifier, String id)
    {
        super(propertiesModifier.apply(new Properties().maxStackSize(1).group(MAIN_ITEM_GROUP)));
        this.setRegistryName(RandomDimensions.ID, id);
    }
}