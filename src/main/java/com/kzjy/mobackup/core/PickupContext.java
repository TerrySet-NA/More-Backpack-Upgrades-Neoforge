package com.kzjy.mobackup.core;

import net.minecraft.world.entity.player.Player;

/**
 * 拾取上下文
 * 使用 ThreadLocal 在事件週期中傳遞玩家實體
 */
public final class PickupContext {
    private static final ThreadLocal<Player> TL = new ThreadLocal<>();
    
    public static void push(Player p) { 
        TL.set(p); 
    }
    
    public static Player current() { 
        return TL.get(); 
    }
    
    public static void pop() { 
        TL.remove(); 
    }
    
    private PickupContext() {}
}