package com.fanwe.shop;

import com.fanwe.library.common.SDHandlerManager;
import com.fanwe.live.LiveConstant;
import com.fanwe.live.activity.room.ILiveActivity;
import com.fanwe.live.business.LiveBaseBusiness;
import com.fanwe.live.model.custommsg.MsgModel;
import com.fanwe.shop.model.custommsg.CustomMsgShopBuySuc;
import com.fanwe.shop.model.custommsg.CustomMsgShopPush;

/**
 * Created by Administrator on 2016/12/28.
 */

public class ShopBusiness extends LiveBaseBusiness
{
    public static final long delayTime = 10000;

    private ShopBusinessListener shopBusinessListener;

    public ShopBusiness(ILiveActivity liveActivity)
    {
        super(liveActivity);
    }

    public void setShopBusinessListener(ShopBusinessListener shopBusinessListener)
    {
        this.shopBusinessListener = shopBusinessListener;
    }

    public void onMsgShop(MsgModel msg)
    {
        if (msg.getCustomMsgType() == LiveConstant.CustomMsgType.MSG_SHOP_GOODS_PUSH)
        {
            onMsgShopGoodsPush(msg.getCustomMsgShopPush());
            startShowGoodsViewRunnable(delayTime);
        } else if (msg.getCustomMsgType() == LiveConstant.CustomMsgType.MSG_SHOP_GOODS_BUY_SUCCESS)
        {
            onMsgShopGoodsBuySuccess(msg.getCustomMsgShopBuySuc());
            startShowGoodsViewRunnable(delayTime);
        }
    }

    /**
     * 定时器定时移除view
     *
     * @param delay
     */
    public void startShowGoodsViewRunnable(long delay)
    {
        stopShowGoodsViewRunnable();
        SDHandlerManager.getMainHandler().postDelayed(dismissRunnable, delay);
    }

    public void stopShowGoodsViewRunnable()
    {
        SDHandlerManager.getMainHandler().removeCallbacks(dismissRunnable);
    }

    private Runnable dismissRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            shopBusinessListener.onShopCountdownEnd();
        }
    };

    protected void onMsgShopGoodsPush(CustomMsgShopPush customMsgShopPush)
    {
        shopBusinessListener.onShopMsgShopPush(customMsgShopPush);
    }

    protected void onMsgShopGoodsBuySuccess(CustomMsgShopBuySuc customMsgShopBuySuc)
    {
        shopBusinessListener.onShopMsgShopBuySuc(customMsgShopBuySuc);
    }

    @Override
    protected BaseBusinessCallback getBaseBusinessCallback()
    {
        return shopBusinessListener;
    }

    public interface ShopBusinessListener extends BaseBusinessCallback
    {
        void onShopMsgShopPush(CustomMsgShopPush customMsgShopPush);

        void onShopMsgShopBuySuc(CustomMsgShopBuySuc customMsgShopBuySuc);

        void onShopCountdownEnd();
    }
}
