package mrp_v2.randomdimensions.common.capabilities;

import mrp_v2.randomdimensions.util.ObjectHolder;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class PortalDataProvider implements ICapabilitySerializable<ListNBT> {

	private PortalDataHandler portalDataHandler;
	private LazyOptional<PortalDataHandler> portalDataHandlerLazyOptional;

	public PortalDataProvider() {
		this.portalDataHandler = new PortalDataHandler();
		this.portalDataHandlerLazyOptional = LazyOptional.of(() -> this.portalDataHandler);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (cap == CapabilityHandler.PORTAL_DATA_CAPABILITY) {
			return this.portalDataHandlerLazyOptional.cast();
		}
		return LazyOptional.empty();
	}

	@Override
	public ListNBT serializeNBT() {
		return (ListNBT) ObjectHolder.PORTAL_DATA_STORAGE.writeNBT(CapabilityHandler.PORTAL_DATA_CAPABILITY,
				this.portalDataHandler, null);
	}

	@Override
	public void deserializeNBT(ListNBT nbt) {
		ObjectHolder.PORTAL_DATA_STORAGE.readNBT(CapabilityHandler.PORTAL_DATA_CAPABILITY, this.portalDataHandler, null,
				nbt);
	}
}
