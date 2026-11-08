package com.eretenkodaniil.mkm.environment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Purely client-side, distance-cropped vanilla particle cue. No dynamic light,
 * server tick, packet, block entity, post effect, combat hook, or Iris dependency.
 */
public final class RuneSlateBlock extends Block {
    public RuneSlateBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!Boolean.parseBoolean(System.getProperty("mkm.runeParticles", "true"))) return;
        // Vanilla invokes animateTick only for nearby client-side blocks.
        // Expected maximum is one particle per chosen random-display tick.
        if (random.nextInt(12) != 0) return;
        level.addParticle(ParticleTypes.END_ROD,
                pos.getX() + .38 + random.nextDouble() * .24,
                pos.getY() + 1.02,
                pos.getZ() + .38 + random.nextDouble() * .24,
                (random.nextDouble() - .5) * .012, .018,
                (random.nextDouble() - .5) * .012);
    }
}
