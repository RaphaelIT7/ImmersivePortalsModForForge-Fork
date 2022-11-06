package qouteall.imm_ptl.core.portal.animation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.dimension.DimId;
import qouteall.q_misc_util.my_util.DQuaternion;

/**
 * {@link PortalState} but one-sided.
 * PortalState contains the information both this-side and other-side.
 * UnilateralPortalState is either the this-side state or other-side state.
 */
public record UnilateralPortalState(
    ResourceKey<Level> dimension,
    Vec3 point,
    DQuaternion orientation,
    double width,
    double height
) {
    // its inverse is itself
    private static final DQuaternion flipAxisH = DQuaternion.rotationByDegrees(
        new Vec3(1, 0, 0), 180
    ).fixFloatingPointErrorAccumulation();
    
    public static UnilateralPortalState extractThisSide(PortalState portalState) {
        return new UnilateralPortalState(
            portalState.fromWorld,
            portalState.fromPos,
            portalState.orientation,
            portalState.width,
            portalState.height
        );
    }
    
    public static UnilateralPortalState extractOtherSide(PortalState portalState) {
        DQuaternion otherSideOrientation = portalState.rotation
            .hamiltonProduct(portalState.orientation)
            .hamiltonProduct(flipAxisH);
        return new UnilateralPortalState(
            portalState.toWorld,
            portalState.toPos,
            otherSideOrientation,
            portalState.width,
            portalState.height
        );
    }
    
    public static PortalState combine(
        UnilateralPortalState thisSide,
        UnilateralPortalState otherSide
    ) {
        Vec3 axisW = McHelper.getAxisWFromOrientation(thisSide.orientation);
        Vec3 axisH = McHelper.getAxisHFromOrientation(thisSide.orientation);
        
        // otherSideOrientation * axis = rotation * thisSideOrientation * flipAxisH * axis
        // otherSideOrientation = rotation * thisSideOrientation * flipAxisH
        // rotation = otherSideOrientation * flipAxisH^-1 * thisSideOrientation^-1
        
        DQuaternion rotation = otherSide.orientation
            .hamiltonProduct(flipAxisH)
            .hamiltonProduct(thisSide.orientation.getConjugated());
        
        double scale = otherSide.width / thisSide.width;
        // ignore other side's aspect ratio changing
        
        return new PortalState(
            thisSide.dimension,
            thisSide.point,
            otherSide.dimension,
            otherSide.point,
            scale,
            rotation,
            thisSide.orientation,
            thisSide.width,
            thisSide.height
        );
    }
    
    public static UnilateralPortalState interpolate(
        UnilateralPortalState from,
        UnilateralPortalState to,
        double progress
    ) {
        return new UnilateralPortalState(
            from.dimension,
            Helper.interpolatePos(from.point, to.point, progress),
            DQuaternion.interpolate(from.orientation, to.orientation, progress),
            Mth.lerp(progress, from.width, to.width),
            Mth.lerp(progress, from.height, to.height)
        );
    }
    
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", dimension.location().toString());
        Helper.putVec3d(tag, "point", point);
        tag.put("orientation", orientation.toTag());
        tag.putDouble("width", width);
        tag.putDouble("height", height);
        return tag;
    }
    
    public static UnilateralPortalState fromTag(CompoundTag tag) {
        ResourceKey<Level> dimension = DimId.idToKey(tag.getString("dimension"));
        Vec3 point = Helper.getVec3d(tag, "point");
        DQuaternion orientation = DQuaternion.fromTag(tag.getCompound("orientation"));
        double width = tag.getDouble("width");
        double height = tag.getDouble("height");
        return new UnilateralPortalState(
            dimension, point, orientation, width, height
        );
    }
    
    /**
     * A mutable version of {@link UnilateralPortalState}.
     */
    public static class Builder {
        public ResourceKey<Level> dimension;
        public Vec3 point;
        public DQuaternion orientation;
        public double width;
        public double height;
        
        public UnilateralPortalState build() {
            return new UnilateralPortalState(
                dimension,
                point,
                orientation,
                width,
                height
            );
        }
        
        public Builder dimension(ResourceKey<Level> dimension) {
            this.dimension = dimension;
            return this;
        }
        
        public Builder point(Vec3 point) {
            this.point = point;
            return this;
        }
        
        public Builder orientation(DQuaternion orientation) {
            this.orientation = orientation;
            return this;
        }
        
        public Builder width(double width) {
            this.width = width;
            return this;
        }
        
        public Builder height(double height) {
            this.height = height;
            return this;
        }
        
        public Builder from(UnilateralPortalState other) {
            this.dimension = other.dimension;
            this.point = other.point;
            this.orientation = other.orientation;
            this.width = other.width;
            this.height = other.height;
            return this;
        }
    }
}
