package moe.nightfall.vic.integratedcircuits.gate;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

import io.netty.buffer.ByteBuf;
import moe.nightfall.vic.integratedcircuits.Config;
import moe.nightfall.vic.integratedcircuits.Content;
import moe.nightfall.vic.integratedcircuits.api.IntegratedCircuitsAPI;
import moe.nightfall.vic.integratedcircuits.api.gate.IGate;
import moe.nightfall.vic.integratedcircuits.api.gate.ISocket;
import moe.nightfall.vic.integratedcircuits.api.gate.ISocket.EnumConnectionType;
import moe.nightfall.vic.integratedcircuits.misc.Cube;
import moe.nightfall.vic.integratedcircuits.misc.MiscUtils;
import moe.nightfall.vic.integratedcircuits.net.Packet7SegmentOpenGui;
import moe.nightfall.vic.integratedcircuits.proxy.CommonProxy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.common.util.Constants.NBT;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

public class Gate7Segment extends Gate {
	
	private final Cube dimensions = new Cube(2, 0, 1, 14, 3, 15);
	
	public int digit = NUMBERS[0];
	public int color;
	public boolean isSlave;
	public boolean hasSlaves;
	public int mode = MODE_SIMPLE;

	public BlockPos parent;

	// TODO Relative positions, will break upon moving like this
	public ArrayList<BlockPos> slaves = Lists.newArrayList();

	//   0
	//   --
	// 5|6 |1
	//   --
	// 4|3 |2
	//   --   # 7
	public static final byte[] NUMBERS = { 63, 6, 91, 79, 102, 109, 125, 7, 127, 111, 119, 124, 57, 94, 121, 113 }; // 0-F

	// In reverse order
	public static final byte[] TRUE = { 121, 62, 80, 120 }; // true
	public static final byte[] FALSE = { 121, 109, 56, 119, 113 }; // false
	public static final byte[] NAN = { 55, 119, 55 };
	public static final byte[] INF = { 113, 55, 48 };
	public static final byte[] INFINITY = { 110, 120, 48, 55, 48, 113, 55, 48 };

	public static final int DOT = 1 << 7;
	public static final int SIGN = 1 << 6;

	public static final int MODE_SIMPLE = 0;
	public static final int MODE_ANALOG = 1;
	public static final int MODE_SHORT_SIGNED = 2;
	public static final int MODE_SHORT_UNSIGNED = 3;
	public static final int MODE_FLOAT = 4;
	public static final int MODE_BINARY_STRING = 5;
	public static final int MODE_MANUAL = 6;

	@Override
	public void preparePlacement(EntityPlayer player, ItemStack stack) {
		color = stack.getItemDamage();
	}

	@Override
	public void onActivatedWithScrewdriver(EntityPlayer player, RayTraceResult hit, ItemStack item) {
		if (player.isSneaking())
			CommonProxy.networkWrapper.sendTo(new Packet7SegmentOpenGui(provider), (EntityPlayerMP) player);
		else
			super.onActivatedWithScrewdriver(player, hit, item);
	}

	@Override
	public void onAdded() {
		super.onAdded();
		if (provider.getWorld().isRemote)
			return;

		isSlave = false;
		EnumFacing abs = provider.getSide().rotateAround(provider.getRotationAbs(EnumFacing.SOUTH).getAxis());
		BlockPos pos = provider.getPos();
		BlockPos pos2 = new BlockPos(pos);
		Gate7Segment seg;

		int off = 0;
		do {
			off++;
			pos2.offset(abs);
			seg = getSegment(pos2);
			if (seg == null || seg.provider.getRotation() != seg.provider.getRotation())
				break;
			if (seg.isSlave)
				continue;

			parent = pos2;
			isSlave = true;

			seg.claimSlaves();
			seg.updateSlaves();

			break;
		} while (off < Config.sevenSegmentMaxDigits);

		sendChangesToClient();
	}

	@Override
	public void onRemoved() {
		super.onRemoved();
		if (!provider.getWorld().isRemote)
			updateConnections();
	}

	@Override
	public void onRotated() {
		updateConnections();
		claimSlaves();
	}

	public void updateConnections() {
		if (isSlave) {
			BlockPos crd = provider.getPos();
			isSlave = false;

			Gate7Segment master = getSegment(parent);
			if (master != null)
				master.claimSlaves();
			EnumFacing abs = provider.getSide().rotateAround(provider.getRotationAbs(EnumFacing.UP).getAxis());
			crd.offset(abs);
			Gate7Segment seg = getSegment(crd);
			if (seg != null)
				seg.claimSlaves();
		} else {
			EnumFacing abs = provider.getSide().rotateAround(provider.getRotationAbs(EnumFacing.UP).getAxis());
			BlockPos crd = provider.getPos().offset(abs);
			if (slaves.contains(crd)) {
				Gate7Segment seg = getSegment(crd);
				if (seg != null)
					seg.claimSlaves();
			}
			slaves.clear();
		}
	}

	public void claimSlaves() {
		isSlave = false;
		slaves.clear();

		EnumFacing abs = provider.getSide().rotateAround(provider.getRotationAbs(EnumFacing.UP).getAxis());
		BlockPos pos = provider.getPos();
		BlockPos pos2 = new BlockPos(pos);
		Gate7Segment seg;

		int off = 0;
		do {
			off++;
			pos2.offset(abs);
			seg = getSegment(pos2);
			if (seg == null)
				break;
			if (seg.isSlave && seg.provider.getRotation() == provider.getRotation())
				slaves.add(new BlockPos(pos2));
			else
				break;
		} while (off < Config.sevenSegmentMaxDigits);

		updateSlaves();
		sendChangesToClient();
	}

	public Gate7Segment getSegment(BlockPos crd) {
		ISocket socket = IntegratedCircuitsAPI.getSocketAt(provider.getWorld(), crd, provider.getSide());
		if (socket == null)
			return null;
		IGate gate = socket.getGate();
		if (gate instanceof Gate7Segment)
			return (Gate7Segment) gate;
		return null;
	}

	private void updateSlaves() {
		if (provider.getWorld().isRemote)
			return;

		int input = 0;

		if (mode == MODE_SIMPLE || mode == MODE_ANALOG) {
			if (mode == MODE_SIMPLE) {
				for (byte[] in : provider.getInput())
					input |= in[0] != 0 ? 1 : 0;
			} else {
				for (byte[] in : provider.getInput())
					if (in[0] > input)
						input = in[0];
			}

			if (slaves.size() < 4 || mode == MODE_ANALOG) {
				writeDigits(null);
				writeDigit(NUMBERS[input]);
			} else {
				byte[] digits = input == 0 ? FALSE : TRUE;
				writeDigits(digits);
			}
		} else {
			boolean sign = false;
			int length = 16;

			for (byte[] in : provider.getInput()) {
				int i2 = 0;
				for (int i = 0; i < 16; i++)
					i2 |= (in[i] != 0 ? 1 : 0) << i;
				input |= i2;
			}

			boolean outOfBounds = false;
			int decimalDot = -1;
			String dispString = "";

			if (isSigned()) {
				sign = (input & 32768) != 0;
				input &= 32767;
			}

			if (mode == MODE_MANUAL) {
				writeDigits(null);
				writeDigit(input & 255);
				return;
			} else if (mode == MODE_FLOAT) {
				float conv = MiscUtils.toBinary16Float(input);
				if (Float.isNaN(conv) || Float.isInfinite(conv)) {
					byte[] digits = null;
					if (Float.isNaN(conv) && slaves.size() > 1)
						digits = NAN;
					else if (Float.isInfinite(conv)) {
						if (slaves.size() > 7)
							digits = INFINITY;
						else if (slaves.size() > 2)
							digits = INF;
					}

					if (digits != null) {
						writeDigits(digits);
						if (sign && Float.isInfinite(conv)) {
							Gate7Segment slave = getSegment(slaves.get(slaves.size() - 1));
							if (slave != null)
								slave.writeDigit(SIGN);
						}
						return;
					} else
						outOfBounds = true;
				} else {
					int size = slaves.size() - String.valueOf((int) conv).length();
					DecimalFormat df = new DecimalFormat("#", new DecimalFormatSymbols(Locale.ENGLISH));
					df.setMaximumFractionDigits(size);
					dispString = df.format(conv);
					decimalDot = dispString.indexOf(".");
					dispString = dispString.replace(".", "");
					if (decimalDot != -1)
						decimalDot = dispString.length() - decimalDot;
				}
			} else if (mode == MODE_BINARY_STRING)
				dispString = Integer.toBinaryString(input);
			else
				dispString = String.valueOf(input);

			int size = dispString.length() - 1;
			if (size > slaves.size() - (isSigned() ? 1 : 0))
				outOfBounds = true;

			dispString = StringUtils.reverse(dispString);
			for (int i = 0; i <= slaves.size(); i++) {
				int decimal = i < dispString.length() ? Integer.valueOf(String.valueOf(dispString.charAt(i))) : 0;
				Gate7Segment slave = this;
				if (i > 0) {
					BlockPos bc = slaves.get(i - 1);
					slave = getSegment(bc);
				}
				if (slave != null) {
					if (i == slaves.size() && isSigned())
						slave.writeDigit(sign ? SIGN : 0);
					else
						slave.writeDigit(outOfBounds ? SIGN : NUMBERS[decimal] | (decimalDot == i ? DOT : 0));
				}
			}
		}
	}

	private void writeDigits(byte[] digits) {
		for (int i = 0; i <= slaves.size(); i++) {
			Gate7Segment slave = this;
			int digit = digits != null && i < digits.length ? digits[i] : 0;
			if (i > 0) {
				BlockPos bc = slaves.get(i - 1);
				slave = getSegment(bc);
			}
			if (slave != null)
				slave.writeDigit(digit);
		}
	}

	private void writeDigit(int digit) {
		if (this.digit != digit) {
			this.digit = digit;
			provider.getWriteStream(10).writeInt(digit);
		}
	}

	private boolean isSigned() {
		return mode == MODE_SHORT_SIGNED || mode == MODE_FLOAT;
	}

	private void sendChangesToClient() {
		provider.notifyPartChange();
		hasSlaves = slaves.size() > 0;
		ByteBuf out = provider.getWriteStream(11);
		out.writeBoolean(isSlave);
		out.writeBoolean(hasSlaves);
	}

	@Override
	public void updateInputPost() {
		super.updateInputPost();
		if (!isSlave)
			updateSlaves();
	}

	@Override
	public void load(NBTTagCompound tag) {
		super.load(tag);
		digit = tag.getInteger("display");
		isSlave = tag.getBoolean("isSlave");
		color = tag.getInteger("color");
		mode = tag.getInteger("mode");
		if (isSlave) {
			int[] pos = tag.getIntArray("parent");
			parent = new BlockPos(pos[0], pos[1], pos[2]);
		}
		else {
			this.slaves = Lists.newArrayList();
			NBTTagList slaves = tag.getTagList("slaves", NBT.TAG_INT_ARRAY);
			for (int i = 0; i < slaves.tagCount(); i++) {
				int[] pos = slaves.getIntArrayAt(i);
				this.slaves.add(new BlockPos(pos[0], pos[1], pos[2]));
			}
		}
	}

	@Override
	public void save(NBTTagCompound tag) {
		super.save(tag);
		tag.setInteger("display", digit);
		tag.setBoolean("isSlave", isSlave);
		tag.setInteger("color", color);
		tag.setInteger("mode", mode);
		if (isSlave)
			tag.setIntArray("parent", new int[] {parent.getX(), parent.getY(), parent.getZ()});
		else {
			NBTTagList slaves = new NBTTagList();
			for (BlockPos slave : this.slaves)
				slaves.appendTag(new NBTTagIntArray(new int[] {slave.getX(), slave.getY(), slave.getZ()}));
			tag.setTag("slaves", slaves);
		}
	}

	@Override
	public void readDesc(NBTTagCompound compound) {
		digit = compound.getInteger("digit");
		color = compound.getInteger("color");
		isSlave = compound.getBoolean("isSlave");
		hasSlaves = compound.getBoolean("hasSlaves");
		mode = compound.getInteger("mode");
	}

	@Override
	public void writeDesc(NBTTagCompound compound) {
		compound.setInteger("digit", digit);
		compound.setInteger("color", color);
		compound.setBoolean("isSlave", isSlave);
		compound.setBoolean("hasSlaves", slaves.size() > 0);
		compound.setInteger("mode", mode);
	}

	@Override
	public void read(byte discr, ByteBuf packet) {
		if (discr == 10)
			digit = packet.readInt();
		else if (discr == 11) {
			isSlave = packet.readBoolean();
			hasSlaves = packet.readBoolean();
			provider.markRender();
		} else
			super.read(discr, packet);
	}

	@Override
	public ItemStack getItemStack() {
		return new ItemStack(Content.item7Segment, 1, color);
	}

	@Override
	public EnumConnectionType getConnectionTypeAtSide(EnumFacing side) {
		return isSlave || (hasSlaves && side == EnumFacing.EAST) ? EnumConnectionType.NONE : mode == MODE_SIMPLE ? EnumConnectionType.SIMPLE : mode == MODE_ANALOG ? EnumConnectionType.ANALOG : EnumConnectionType.BUNDLED;
	}

	@Override
	public Cube getDimension() {
		return dimensions;
	}
}
