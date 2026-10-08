package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.manager.LeavesModule;
import com.dev.leavesHack.utils.combat.CombatUtil;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class PushCrystal extends LeavesModule {
    public static PushCrystal INSTANCE;


    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSwitch = settings.createGroup("Switch");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("TargetRange")
        .description("目标范围")
        .defaultValue(6.0)
        .min(1.0)
        .sliderMax(16.0)
        .build()
    );
    private final Setting<Double> searchRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("SearchRange")
        .description("最终水晶位置搜索范围")
        .defaultValue(5.0)
        .min(1.0)
        .sliderMax(8.0)
        .build()
    );
    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("PlaceRange")
        .description("方块放置范围")
        .defaultValue(4.5)
        .min(1.0)
        .sliderMax(6.0)
        .build()
    );
    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("MinDamage")
        .description("最低目标伤害")
        .defaultValue(4.0)
        .min(0.0)
        .sliderMax(36.0)
        .build()
    );
    private final Setting<Double> maxSelfDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("MaxSelfDamage")
        .description("最大自身伤害")
        .defaultValue(12.0)
        .min(0.0)
        .sliderMax(36.0)
        .build()
    );
    private final Setting<Boolean> noSuicide = sgGeneral.add(new BoolSetting.Builder()
        .name("NoSuicide")
        .description("避免自杀")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> actionDelay = sgGeneral.add(new IntSetting.Builder()
        .name("ActionDelay")
        .description("动作间隔 tick")
        .defaultValue(3)
        .min(0)
        .sliderMax(10)
        .build()
    );
    private final Setting<Integer> breakDelay = sgGeneral.add(new IntSetting.Builder()
        .name("BreakDelay")
        .description("破坏延迟")
        .defaultValue(200)
        .min(0)
        .sliderMax(500)
        .build()
    );
    private final Setting<Boolean> preferCrystal = sgGeneral.add(new BoolSetting.Builder()
        .name("PreferCrystal")
        .description("优先水晶")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> pauseWhileUsing = sgGeneral.add(new BoolSetting.Builder()
        .name("PauseWhileUsing")
        .description("使用物品时暂停")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("Rotate")
        .description("转头")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> packetPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("PacketPlace")
        .description("发包放置")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> mine = sgGeneral.add(new BoolSetting.Builder()
        .name("Mine")
        .description("自动挖掘")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> yawDeceive = sgGeneral.add(new BoolSetting.Builder()
        .name("YawDeceive")
        .description("欺骗朝向")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> maxBuildBlocks = sgGeneral.add(new IntSetting.Builder()
        .name("MaxBuildBlocks")
        .description("最大模拟方块数")
        .defaultValue(5)
        .min(1)
        .sliderMax(9)
        .build()
    );
    private final Setting<Boolean> inventorySwap = sgSwitch.add(new BoolSetting.Builder()
        .name("InventorySwap")
        .description("背包鬼手")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("Render")
        .description("渲染")
        .defaultValue(true)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("ShapeMode")
        .description("渲染模式")
        .defaultValue(ShapeMode.Both)
        .build()
    );
    private final Setting<SettingColor> crystalColor = sgRender.add(new ColorSetting.Builder()
        .name("CrystalColor")
        .description("水晶位置颜色")
        .defaultValue(new SettingColor(255, 0, 0, 80))
        .build()
    );
    private final Setting<SettingColor> pistonColor = sgRender.add(new ColorSetting.Builder()
        .name("PistonColor")
        .description("活塞位置颜色")
        .defaultValue(new SettingColor(255, 255, 255, 80))
        .build()
    );
    private final Setting<SettingColor> redstoneColor = sgRender.add(new ColorSetting.Builder()
        .name("RedstoneColor")
        .description("红石位置颜色")
        .defaultValue(new SettingColor(255, 100, 0, 80))
        .build()
    );

    private PlayerEntity target;
    private PlayerEntity lastTarget;
    private BlockPos lastBestPos;
    private CrystalPushPlan currentPlan;
    private BlockPos pendingBreakPos;
    private int pendingBreakTicks;
    private BlockPos lastPiston;
    private Direction lastPistonDirection;
    private int actionCooldown;
    private final Timer crystalBreakTimer = new Timer();

    public PushCrystal() {
        super(LeavesHack.LEAVES_COMBAT, "PushCrystal", "活塞水晶");
        INSTANCE = this;
    }

    public void onActivate() {
        target = null;
        lastTarget = null;
        lastBestPos = null;
        currentPlan = null;
        pendingBreakPos = null;
        pendingBreakTicks = 0;
        lastPiston = null;
        lastPistonDirection = null;
        actionCooldown = 0;
        crystalBreakTimer.setMs(99999999);
    }

    public void onDeactivate() {
        target = null;
        lastTarget = null;
        lastBestPos = null;
        currentPlan = null;
        pendingBreakPos = null;
        lastPiston = null;
        lastPistonDirection = null;
    }

    @Override
    public String getInfoString() {
        return target == null ? null : "[" + target.getName().getString() + "]";
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        if (preferCrystal.get() && CrystalAuraPlus.INSTANCE.renderCrystalPos != null) return;
        if (mine.get() && lastPiston != null && lastPistonDirection != null) {
            if (BlockUtil.getBlock(lastPiston) instanceof PistonBlock
                && BlockUtil.getBlock(lastPiston.offset(lastPistonDirection)) instanceof PistonHeadBlock) {
                Direction side = BlockUtil.getClickSide(lastPiston);
                if (side != null) {
                    mc.interactionManager.attackBlock(lastPiston, side);
                    lastPiston = null;
                    lastPistonDirection = null;
                }
            }
        }
        target = CombatUtil.getClosestEnemy(targetRange.get());
        if (target == null) {
            lastTarget = null;
            lastBestPos = null;
            currentPlan = null;
            pendingBreakPos = null;
            return;
        }
        if (target != lastTarget) {
            lastTarget = target;
            lastBestPos = null;
        }
        if (pendingBreakPos != null) {
            pendingBreakTicks++;
            if (BlockUtil.hasCrystalPlaceAccurate(pendingBreakPos)) {
                CombatUtil.attackCrystal(pendingBreakPos, rotate.get(), false);
                pendingBreakPos = null;
                pendingBreakTicks = 0;
                currentPlan = null;
                actionCooldown = actionDelay.get();
            } else if (pendingBreakTicks > 3) {
                pendingBreakPos = null;
                pendingBreakTicks = 0;
            }
            return;
        }

        if (actionCooldown > 0) {
            actionCooldown--;
            return;
        }
        if (pauseWhileUsing.get() && (mc.player.isUsingItem() || mc.options.useKey.isPressed())) return;
        int crystalSlot = findItem(Items.END_CRYSTAL);
        int pistonSlot = findClass(PistonBlock.class);
        int redstoneSlot = findItem(Items.REDSTONE_BLOCK);
        if (crystalSlot == -1 || pistonSlot == -1 || redstoneSlot == -1) return;
        CrystalPushPlan plan = findPlan(target);
        currentPlan = plan;
        if (plan == null) return;
        if (breakBlockingCrystal(plan.pos2())) return;
        if (execute(plan, crystalSlot, pistonSlot, redstoneSlot)) {
            pendingBreakPos = plan.pos1();
            pendingBreakTicks = 0;
            actionCooldown = actionDelay.get();
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || currentPlan == null) return;
        event.renderer.box(currentPlan.pos1(), crystalColor.get(), crystalColor.get(), shapeMode.get(), 0);
        for (BlockPos pos : currentPlan.pistonPositions()) {
            event.renderer.box(pos, pistonColor.get(), pistonColor.get(), shapeMode.get(), 0);
        }
        for (BlockPos pos : currentPlan.redstonePositions()) {
            event.renderer.box(pos, redstoneColor.get(), redstoneColor.get(), shapeMode.get(), 0);
        }
    }

    private CrystalPushPlan findPlan(PlayerEntity target) {
        List<FinalCandidate> candidates = findFinalCandidates(target);
        if (candidates.isEmpty()) {
            lastBestPos = null;
            return null;
        }
        FinalCandidate best = candidates.get(0);
        if (lastBestPos != null) {
            for (int i = 0; i < candidates.size(); i++) {
                FinalCandidate cached = candidates.get(i);
                if (!cached.pos().equals(lastBestPos)) continue;
                if (cached.targetDamage() >= best.targetDamage() * 0.95f) {
                    candidates.remove(i);
                    candidates.add(0, cached);
                }
                break;
            }
        }

        for (FinalCandidate candidate : candidates) {
            BlockPos pos1 = candidate.pos();
            for (Direction direction : HORIZONTAL_DIRECTIONS) {
                if (!yawDeceive.get() && direction != mc.player.getHorizontalFacing()) continue;
                BlockPos pos2 = pos1.offset(direction);
                if (!BlockUtil.canPlaceCrystal(pos2) && !BlockUtil.hasCrystal(pos2)) continue;

                Direction face1 = direction.getOpposite();
                List<BlockPos> pistonCandidates = getPistonCandidates(pos2, face1);
                BuildResult build = new BuildSolver(pos2, face1, pistonCandidates).search();
                if (build == null) continue;

                List<BlockPos> pistons = new ArrayList<>();
                List<BlockPos> redstones = new ArrayList<>();
                for (Placement placement : build.placements()) {
                    if (placement.kind() == BlockKind.PISTON) pistons.add(placement.pos());
                    else redstones.add(placement.pos());
                }
                CrystalPushPlan plan = new CrystalPushPlan(
                    pos1,
                    pos2,
                    face1,
                    List.copyOf(pistons),
                    List.copyOf(redstones),
                    List.copyOf(build.placements()),
                    candidate.targetDamage(),
                    candidate.selfDamage()
                );
                if (!isWithinPlaceRange(plan)) continue;
                lastBestPos = pos1;
                return plan;
            }
        }
        lastBestPos = null;
        return null;
    }

    private List<FinalCandidate> findFinalCandidates(PlayerEntity target) {
        List<FinalCandidate> candidates = new ArrayList<>();
        for (BlockPos candidate : BlockUtil.getSphere(searchRange.get())) {
            boolean hasPlaceablePos2 = false;
            for (Direction direction : HORIZONTAL_DIRECTIONS) {
                BlockPos pos2 = candidate.offset(direction);
                if (BlockUtil.canPlaceCrystal(pos2) || BlockUtil.hasCrystal(pos2)) {
                    hasPlaceablePos2 = true;
                    break;
                }
            }
            if (!hasPlaceablePos2) continue;

            Vec3d explosion = crystalVector(candidate);
            float damage = DamageUtils.crystalDamage(target, explosion);
            float selfDamage = DamageUtils.crystalDamage(mc.player, explosion);
            if (!Float.isFinite(damage) || !Float.isFinite(selfDamage)) continue;
            if (damage < minDamage.get() || selfDamage > maxSelfDamage.get()) continue;
            if (noSuicide.get() && selfDamage > EntityUtils.getTotalHealth(mc.player)) continue;

            candidates.add(new FinalCandidate(
                candidate.toImmutable(),
                damage,
                selfDamage,
                mc.player.getEyePos().squaredDistanceTo(explosion)
            ));
        }
        candidates.sort((a, b) -> {
            int damageOrder = Float.compare(b.targetDamage(), a.targetDamage());
            if (damageOrder != 0) return damageOrder;
            return Double.compare(a.distance(), b.distance());
        });
        return candidates;
    }

    private boolean breakBlockingCrystal(BlockPos pos) {
        if (BlockUtil.canPlaceCrystal(pos)) return false;
        if (!BlockUtil.hasCrystal(pos)) return true;
        if (!crystalBreakTimer.passedMs(breakDelay.get())) return true;
        CombatUtil.attackCrystal(pos, rotate.get(), false);
        crystalBreakTimer.reset();
        actionCooldown = actionDelay.get();
        return true;
    }

    private List<BlockPos> getPistonCandidates(BlockPos pos2, Direction face1) {
        BlockPos plane = pos2.offset(face1.getOpposite());
        Direction horizontal = face1.rotateYClockwise();
        List<BlockPos> candidates = new ArrayList<>(6);
        for (int y = 0; y <= 1; y++) {
            for (int lateral = -1; lateral <= 1; lateral++) {
                candidates.add(plane.up(y).offset(horizontal, lateral));
            }
        }
        return candidates;
    }

    private boolean execute(CrystalPushPlan plan, int crystalSlot, int pistonSlot, int redstoneSlot) {
        Set<BlockPos> planned = new HashSet<>();
        lastPiston = null;
        lastPistonDirection = null;
        for (Placement placement : plan.placements()) {
            BlockState state = mc.world.getBlockState(placement.pos());
            if (isExpectedBlock(state, placement)) {
                // 世界中已有同朝向活塞或红石块时，不重复放置，但仍加入虚拟状态。
                planned.add(placement.pos());
                if (placement.kind() == BlockKind.PISTON) {
                    lastPiston = placement.pos();
                    lastPistonDirection = plan.face1();
                }
                continue;
            }
            if (!canPlaceVirtual(placement.pos(), planned) || !place(placement, planned, placement.kind() == BlockKind.PISTON ? pistonSlot : redstoneSlot)) {
                return false;
            }
            planned.add(placement.pos());
            if (placement.kind() == BlockKind.PISTON) {
                lastPiston = placement.pos();
                lastPistonDirection = plan.face1();
            }
        }
        if (lastPiston == null) {
            for (BlockPos candidate : getPistonCandidates(plan.pos2(), plan.face1())) {
                if (BlockUtil.getBlock(candidate) instanceof PistonBlock) {
                    lastPiston = candidate;
                    lastPistonDirection = plan.face1();
                    break;
                }
            }
        }
        if (!BlockUtil.canPlaceCrystal(plan.pos2())) return false;
        if (!placeCrystal(plan.pos2(), crystalSlot)) return false;
        return true;
    }

    private boolean place(Placement placement, Set<BlockPos> planned, int slot) {
        Direction side = getVirtualPlaceSide(placement.pos(), planned);
        if (side == null || slot == -1) return false;

        int oldSlot = mc.player.getInventory().getSelectedSlot();
        swap(slot, oldSlot);
        if (rotate.get()) {
            Rotation.snapAt(placement.pos().toCenterPos().add(new Vec3d(
                side.getVector().getX() * 0.5,
                side.getVector().getY() * 0.5,
                side.getVector().getZ() * 0.5
            )));
            if (placement.kind() == BlockKind.PISTON && yawDeceive.get()) {
                pistonFacing(placement.facing().getOpposite());
            }
        }
        BlockUtil.placeBlock(placement.pos(), side, false, packetPlace.get());
        if (rotate.get()) Rotation.snapBack();
        swapBack(slot, oldSlot);
        return true;
    }
    public static void pistonFacing(Direction i) {
        if (i == Direction.EAST) {
            Rotation.snapAt(-90.0f, 5.0f);
        } else if (i == Direction.WEST) {
            Rotation.snapAt(90.0f, 5.0f);
        } else if (i == Direction.NORTH) {
            Rotation.snapAt(180.0f, 5.0f);
        } else if (i == Direction.SOUTH) {
            Rotation.snapAt(0.0f, 5.0f);
        }
    }
    private boolean placeCrystal(BlockPos pos, int slot) {
        Direction side = BlockUtil.getClickSide(pos.down());
        if (side == null || slot == -1) return false;

        int oldSlot = mc.player.getInventory().getSelectedSlot();
        swap(slot, oldSlot);
        BlockUtil.clickBlock(pos.down(), side, rotate.get());
        swapBack(slot, oldSlot);
        return true;
    }

    private void swap(int slot, int oldSlot) {
        if (inventorySwap.get()) InventoryUtil.inventorySwap(slot, oldSlot);
        else InventoryUtil.switchToSlot(slot);
    }

    private void swapBack(int slot, int oldSlot) {
        if (inventorySwap.get()) InventoryUtil.inventorySwap(slot, oldSlot);
        else InventoryUtil.switchToSlot(oldSlot);
    }

    private boolean isWithinPlaceRange(CrystalPushPlan plan) {
        if (!PlayerUtils.isWithin(plan.pos2().toCenterPos(), placeRange.get())) return false;
        for (Placement placement : plan.placements()) {
            if (!PlayerUtils.isWithin(placement.pos().toCenterPos(), placeRange.get())) return false;
        }
        return true;
    }

    private boolean isExpectedBlock(BlockState state, Placement placement) {
        return placement.kind() == BlockKind.PISTON
                && state.getBlock() instanceof PistonBlock
                && state.get(PistonBlock.FACING) == placement.facing()
            || placement.kind() == BlockKind.REDSTONE && state.getBlock() instanceof RedstoneBlock;
    }

    private Vec3d crystalVector(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    private int findItem(Item item) {
        return inventorySwap.get() ? InventoryUtil.findItemInventorySlot(item) : InventoryUtil.findItem(item);
    }

    private int findClass(Class<?> itemClass) {
        return inventorySwap.get() ? InventoryUtil.findClassInventory(itemClass) : InventoryUtil.findClass(itemClass);
    }

    private boolean canPlaceVirtual(BlockPos pos, Set<BlockPos> planned) {
        if (planned.contains(pos) || !BlockUtil.canReplace(pos) || BlockUtil.hasEntity(pos, false)) return false;
        return getVirtualPlaceSide(pos, planned) != null;
    }
    private Direction getVirtualPlaceSide(BlockPos pos, Set<BlockPos> planned) {
        Direction realSide = BlockUtil.getPlaceSide(pos, null);
        if (realSide != null) return realSide;

        for (Direction side : Direction.values()) {
            BlockPos support = pos.offset(side);
            if (planned.contains(support) && isVirtualSupportDirection(support, side.getOpposite())) return side;
            if (isSolidSupport(support) && BlockUtil.isGrimDirection(support, side.getOpposite())) return side;
        }
        return null;
    }
    private boolean isVirtualSupportDirection(BlockPos support, Direction direction) {
        double minEyeY = mc.player.getY() + 0.4;
        double maxEyeY = mc.player.getY() + 1.62;
        Box eyePositions = new Box(mc.player.getX(), minEyeY, mc.player.getZ(),
            mc.player.getX(), maxEyeY, mc.player.getZ()).expand(0.0002);
        Box supportBox = new Box(support);

        boolean intersects = supportBox.maxX - 1.0e-7 > eyePositions.minX
            && supportBox.minX + 1.0e-7 < eyePositions.maxX
            && supportBox.maxY - 1.0e-7 > eyePositions.minY
            && supportBox.minY + 1.0e-7 < eyePositions.maxY
            && supportBox.maxZ - 1.0e-7 > eyePositions.minZ
            && supportBox.minZ + 1.0e-7 < eyePositions.maxZ;
        if (intersects) return true;

        return switch (direction) {
            case NORTH -> eyePositions.minZ <= supportBox.minZ;
            case SOUTH -> eyePositions.maxZ >= supportBox.maxZ;
            case EAST -> eyePositions.maxX >= supportBox.maxX;
            case WEST -> eyePositions.minX <= supportBox.minX;
            case UP -> eyePositions.maxY >= supportBox.maxY;
            case DOWN -> eyePositions.minY <= supportBox.minY;
        };
    }

    private boolean isSolidSupport(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.isSolid() || state.getBlock() instanceof RedstoneBlock;
    }

    private static final Direction[] HORIZONTAL_DIRECTIONS = {
        Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    public enum BlockKind {
        PISTON,
        REDSTONE
    }

    private record FinalCandidate(BlockPos pos, float targetDamage, float selfDamage, double distance) {
    }

    public record Placement(BlockPos pos, BlockKind kind, Direction facing) {
        public Placement {
            pos = pos.toImmutable();
        }
    }

    public record CrystalPushPlan(
        BlockPos pos1,
        BlockPos pos2,
        Direction face1,
        List<BlockPos> pistonPositions,
        List<BlockPos> redstonePositions,
        List<Placement> placements,
        float targetDamage,
        float selfDamage
    ) {
        public CrystalPushPlan {
            pos1 = pos1.toImmutable();
            pos2 = pos2.toImmutable();
            pistonPositions = List.copyOf(pistonPositions);
            redstonePositions = List.copyOf(redstonePositions);
            placements = List.copyOf(placements);
        }
    }

    private record BuildResult(List<Placement> placements) {
        private BuildResult {
            placements = List.copyOf(placements);
        }
    }

    private record BuildState(Map<BlockPos, BlockKind> planned, List<Placement> placements) {
        private BuildState {
            planned = Map.copyOf(planned);
            placements = List.copyOf(placements);
        }
    }

    private final class BuildSolver {
        private final BlockPos pos2;
        private final Direction face1;
        private final List<BlockPos> pistonCandidates;
        private final List<BlockPos> redstoneCandidates;

        private BuildSolver(BlockPos pos2, Direction face1, List<BlockPos> pistonCandidates) {
            this.pos2 = pos2;
            this.face1 = face1;
            this.pistonCandidates = pistonCandidates;
            this.redstoneCandidates = getRedstoneCandidates(pistonCandidates, face1);
        }

        private BuildResult search() {
            BuildState initial = new BuildState(Map.of(), List.of());
            Queue<BuildState> queue = new ArrayDeque<>();
            Set<String> visited = new HashSet<>();
            queue.add(initial);
            visited.add(key(initial.planned()));

            while (!queue.isEmpty()) {
                BuildState state = queue.remove();
                if (isComplete(state.planned())) return new BuildResult(state.placements());
                if (state.placements().size() >= maxBuildBlocks.get()) continue;

                for (BlockPos candidate : pistonCandidates) {
                    if (state.planned().containsKey(candidate) || isPistonAt(candidate)) continue;
                    if (!canPlaceVirtual(candidate, state.planned().keySet())) continue;
                    BuildState next = add(state, candidate, BlockKind.PISTON);
                    if (visited.add(key(next.planned()))) queue.add(next);
                }
                for (BlockPos candidate : redstoneCandidates) {
                    if (state.planned().containsKey(candidate) || isRedstoneAt(candidate)) continue;
                    if (!canPlaceVirtual(candidate, state.planned().keySet())) continue;
                    BuildState next = add(state, candidate, BlockKind.REDSTONE);
                    if (visited.add(key(next.planned()))) queue.add(next);
                }
            }
            return null;
        }

        private BuildState add(BuildState state, BlockPos pos, BlockKind kind) {
            Map<BlockPos, BlockKind> planned = new HashMap<>(state.planned());
            planned.put(pos, kind);
            List<Placement> placements = new ArrayList<>(state.placements());
            placements.add(new Placement(pos, kind, kind == BlockKind.PISTON ? face1 : null));
            return new BuildState(planned, placements);
        }

        private boolean isComplete(Map<BlockPos, BlockKind> planned) {
            for (BlockPos piston : pistonCandidates) {
                boolean pistonReady = isPistonAt(piston)
                    || planned.get(piston) == BlockKind.PISTON;
                if (!pistonReady) continue;

                for (Direction side : Direction.values()) {
                    BlockPos redstone = piston.offset(side);
                    if (redstone.equals(pos2)) continue;
                    if (isRedstoneAt(redstone) || planned.get(redstone) == BlockKind.REDSTONE) return true;
                }
            }
            return false;
        }

        private List<BlockPos> getRedstoneCandidates(List<BlockPos> pistons, Direction facing) {
            Set<BlockPos> result = new LinkedHashSet<>();
            Set<BlockPos> pistonSet = new HashSet<>(pistons);
            for (BlockPos piston : pistons) {
                for (Direction side : Direction.values()) {
                    if (side == facing) continue;
                    BlockPos redstone = piston.offset(side);
                    if (redstone.equals(pos2) || pistonSet.contains(redstone)) continue;
                    if (isRedstoneAt(redstone) || BlockUtil.canReplace(redstone)) result.add(redstone);
                }
            }
            return new ArrayList<>(result);
        }

        private boolean isPistonAt(BlockPos pos) {
            BlockState state = mc.world.getBlockState(pos);
            return state.getBlock() instanceof PistonBlock && state.get(PistonBlock.FACING) == face1;
        }

        private boolean isRedstoneAt(BlockPos pos) {
            return mc.world.getBlockState(pos).getBlock() == Blocks.REDSTONE_BLOCK;
        }

        private String key(Map<BlockPos, BlockKind> planned) {
            List<String> entries = new ArrayList<>();
            for (Map.Entry<BlockPos, BlockKind> entry : planned.entrySet()) {
                BlockPos pos = entry.getKey();
                entries.add(pos.getX() + ":" + pos.getY() + ":" + pos.getZ() + ":" + entry.getValue());
            }
            Collections.sort(entries);
            return String.join("|", entries);
        }
    }
}
