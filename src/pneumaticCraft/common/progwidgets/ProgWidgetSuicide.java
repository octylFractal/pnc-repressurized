package pneumaticCraft.common.progwidgets;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import pneumaticCraft.common.entity.living.EntityDrone;
import pneumaticCraft.common.item.ItemPlasticPlants;
import pneumaticCraft.lib.Textures;

public class ProgWidgetSuicide extends ProgWidget{

    @Override
    public boolean hasStepInput(){
        return true;
    }

    @Override
    public boolean hasStepOutput(){
        return false;
    }

    @Override
    public int getWidth(){
        return 40;
    }

    @Override
    public Class<? extends IProgWidget> returnType(){
        return null;
    }

    @Override
    public Class<? extends IProgWidget>[] getParameters(){
        return null;
    }

    @Override
    public String getWidgetString(){
        return "suicide";
    }

    @Override
    public String getGuiTabText(){
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getGuiTabColor(){
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getCraftingColorIndex(){
        return ItemPlasticPlants.REPULSION_PLANT_DAMAGE;
    }

    @Override
    public WidgetCategory getCategory(){
        return WidgetCategory.ACTION;
    }

    @Override
    protected ResourceLocation getTexture(){
        return Textures.PROG_WIDGET_SUICIDE;
    }

    @Override
    public EntityAIBase getWidgetAI(EntityDrone drone, IProgWidget widget){
        return new DroneAISuicide(drone);
    }

    private static class DroneAISuicide extends EntityAIBase{
        private final EntityDrone drone;

        public DroneAISuicide(EntityDrone drone){
            this.drone = drone;
        }

        @Override
        public boolean shouldExecute(){
            drone.setCustomNameTag("");
            drone.attackEntityFrom(DamageSource.outOfWorld, 2000.0F);
            return false;
        }
    }
}
