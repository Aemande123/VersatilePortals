package mrp_v2.randomdimensions.village;

import mrp_v2.randomdimensions.RandomDimensions;
import mrp_v2.randomdimensions.util.ObjectHolder;
import net.minecraft.village.PointOfInterestType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PortalPointOfInterestType extends PointOfInterestType
{
    public static final String ID = "portal";

    public PortalPointOfInterestType()
    {
        super(ID, ObjectHolder.getPortalBlockStates(), 0, 1);
        this.setRegistryName(RandomDimensions.ID, ID);
    }

    public PortalPointOfInterestType register() throws InvocationTargetException, IllegalAccessException
    {
        Method registerBlockStatesMethod = null;
        for (Method method : PointOfInterestType.class.getDeclaredMethods())
        {
            if (method.getParameterCount() != 1)
            {
                continue;
            }
            if (method.getReturnType() != PointOfInterestType.class)
            {
                continue;
            }
            if (method.getParameters()[0].getType() != PointOfInterestType.class)
            {
                continue;
            }
            registerBlockStatesMethod = method;
            break;
        }
        registerBlockStatesMethod.setAccessible(true);
        registerBlockStatesMethod.invoke(null, this);
        return this;
    }
}
