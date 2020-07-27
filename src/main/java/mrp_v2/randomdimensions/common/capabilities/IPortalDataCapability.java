package mrp_v2.randomdimensions.common.capabilities;

import java.util.Set;

import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3d;

public interface IPortalDataCapability {

	public Vector3d getLastPortalVec(String worldID);

	public void setLastPortalVec(String worldID, Vector3d lastPortalVec);

	public Direction getTeleportDirection(String worldID);

	public void setTeleportDirection(String worldID, Direction teleportDirection);

	public void decrementRemainingPortalCooldown();

	public int getRemainingPortalCooldown();

	public void setRemainingPortalCooldown(int remainingPortalCooldown);

	public int getTimeInPortal();

	public void setTimeInPortal(int timeInPortal);

	public Set<String> getWorldsWithPortalData();
}
