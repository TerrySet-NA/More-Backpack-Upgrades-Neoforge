package com.kzjy.mobackup.core;

import net.minecraft.world.entity.player.Player;

/**
 * 拾取上下文
 * 用于在物品拾取事件过程中传递玩家实例
 * 使得升级 Wrapper 在执行插入逻辑时能够获取到关联的玩家
 */
public final class PickupContext {
    // 使用 ThreadLocal 确保线程安全，防止多线程并发时数据错乱
    private static final ThreadLocal<Player> TL = new ThreadLocal<>();
    
    /**
     * 将玩家实例压入当前线程上下文
     */
    public static void push(Player p) { TL.set(p); }
    
    /**
     * 获取当前线程上下文中的玩家实例
     */
    public static Player current() { return TL.get(); }
    
    /**
     * 清理上下文，避免内存泄漏
     */
    public static void pop() { TL.remove(); }
    
    private PickupContext() {}
}