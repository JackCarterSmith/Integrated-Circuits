package vic.mod.integratedcircuits.misc;

import net.minecraft.item.Item;

public class ItemAmount 
{
	public double amount;
	public Item item;
	
	public ItemAmount(Item item, double amount)
	{
		this.item = item;
		this.amount = amount;
	}
}