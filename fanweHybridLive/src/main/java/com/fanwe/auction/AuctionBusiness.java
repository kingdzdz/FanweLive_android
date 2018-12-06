package com.fanwe.auction;

import android.os.CountDownTimer;
import android.view.View;

import com.fanwe.auction.common.AuctionCommonInterface;
import com.fanwe.auction.model.App_pai_user_get_videoActModel;
import com.fanwe.auction.model.PaiBuyerModel;
import com.fanwe.auction.model.PaiUserGetVideoDataModel;
import com.fanwe.auction.model.PaiUserGoodsDetailDataInfoModel;
import com.fanwe.auction.model.custommsg.CustomMsgAuctionCreateSuccess;
import com.fanwe.auction.model.custommsg.CustomMsgAuctionFail;
import com.fanwe.auction.model.custommsg.CustomMsgAuctionNotifyPay;
import com.fanwe.auction.model.custommsg.CustomMsgAuctionOffer;
import com.fanwe.auction.model.custommsg.CustomMsgAuctionPaySuccess;
import com.fanwe.auction.model.custommsg.CustomMsgAuctionSuccess;
import com.fanwe.hybrid.http.AppRequestCallback;
import com.fanwe.hybrid.http.AppRequestCallbackWrapper;
import com.fanwe.hybrid.model.BaseActModel;
import com.fanwe.library.adapter.http.model.SDResponse;
import com.fanwe.library.utils.SDCollectionUtil;
import com.fanwe.library.utils.SDDateUtil;
import com.fanwe.live.LiveConstant;
import com.fanwe.live.LiveInformation;
import com.fanwe.live.dao.UserModelDao;
import com.fanwe.live.model.UserModel;
import com.fanwe.live.model.custommsg.MsgModel;

import java.util.List;

/**
 * Created by Administrator on 2016/11/30.
 */

public class AuctionBusiness
{

    private CountDownTimer timer;
    private AuctionBusinessListener auctionBusinessListener;
    private boolean isAuctioning;
    private PaiBuyerModel currentBuyer;

    public PaiBuyerModel getCurrentBuyer()
    {
        return currentBuyer;
    }

    public void setAuctionBusinessListener(AuctionBusinessListener auctionBusinessListener)
    {
        this.auctionBusinessListener = auctionBusinessListener;
    }

    private void setAuctioning(boolean auctioning)
    {
        isAuctioning = auctioning;
        LiveInformation.getInstance().setAuctioning(auctioning);
        auctionBusinessListener.onAuctioningChange(isAuctioning);
    }

    public boolean isAuctioning()
    {
        return isAuctioning;
    }

    public void onAuctionMsg(MsgModel msg)
    {
        switch (msg.getCustomMsgType())
        {
            case LiveConstant.CustomMsgType.MSG_AUCTION_CREATE_SUCCESS:
                //创建竞拍成功
                onAuctionMsgCreateSuccess(msg.getCustomMsgAuctionCreateSuccess());
                break;
            case LiveConstant.CustomMsgType.MSG_AUCTION_OFFER:
                //出价
                onAuctionMsgOffer(msg.getCustomMsgAuctionOffer());
                break;
            case LiveConstant.CustomMsgType.MSG_AUCTION_SUCCESS:
                //竞拍成功
                onAuctionMsgSuccess(msg.getCustomMsgAuctionSuccess());
                break;
            case LiveConstant.CustomMsgType.MSG_AUCTION_NOTIFY_PAY:
                //竞拍通知付款，比如第一名超时未付款，通知下一名付款
                onAuctionMsgNotifyPay(msg.getCustomMsgAuctionNotifyPay());
                break;
            case LiveConstant.CustomMsgType.MSG_AUCTION_FAIL:
                //流拍
                onAuctionMsgFail(msg.getCustomMsgAuctionFail());
                break;
            case LiveConstant.CustomMsgType.MSG_AUCTION_PAY_SUCCESS:
                //支付成功
                onAuctionMsgPaySuccess(msg.getCustomMsgAuctionPaySuccess());
                break;
        }
    }

    protected void onAuctionMsgCreateSuccess(CustomMsgAuctionCreateSuccess customMsg)
    {
        requestPaiInfo(customMsg.getPai_id(), null);
        auctionBusinessListener.onAuctionMsgCreateSuccess(customMsg);
    }

    protected void onAuctionMsgOffer(CustomMsgAuctionOffer customMsg)
    {
        auctionBusinessListener.onAuctionMsgOffer(customMsg);
    }

    protected void onAuctionMsgSuccess(CustomMsgAuctionSuccess customMsg)
    {
        setAuctioning(false);
        auctionBusinessListener.onAuctionMsgSuccess(customMsg);
        needShowPay(customMsg.getBuyer());
    }

    protected void onAuctionMsgNotifyPay(CustomMsgAuctionNotifyPay customMsg)
    {
        auctionBusinessListener.onAuctionMsgNotifyPay(customMsg);
        needShowPay(customMsg.getBuyer());
    }

    protected void onAuctionMsgFail(CustomMsgAuctionFail customMsg)
    {
        setAuctioning(false);
        auctionBusinessListener.onAuctionMsgFail(customMsg);
        needShowPay(customMsg.getBuyer());
    }

    protected void onAuctionMsgPaySuccess(CustomMsgAuctionPaySuccess customMsg)
    {
        setAuctioning(false);
        auctionBusinessListener.onAuctionMsgPaySuccess(customMsg);
        needShowPay(customMsg.getBuyer());
    }

    /**
     * 是否显示付款入口
     */
    private void needShowPay(List<PaiBuyerModel> list)
    {
        auctionBusinessListener.onAuctionNeedShowPay(false);
        stopPayRemaining();
        if (SDCollectionUtil.isEmpty(list))
        {
            return;
        }
        UserModel user = UserModelDao.query();
        if (user == null)
        {
            return;
        }

        PaiBuyerModel localBuyer = null;
        for (PaiBuyerModel item : list)
        {
            if (user.getUser_id().equals(item.getUser_id()) && item.getType() == 1)
            {
                //未付款存在倒计时的人和本地登录的人一样
                localBuyer = item;
                break;
            }
        }

        currentBuyer = localBuyer;
        auctionBusinessListener.onAuctionNeedShowPay(currentBuyer != null);
        startPayRemaining();
    }

    /**
     * 付款倒计时
     */
    private void startPayRemaining()
    {
        if (currentBuyer == null)
        {
            return;
        }

        stopPayRemaining();

        long left_time = currentBuyer.getLeft_time();

        timer = new CountDownTimer(left_time * 1000, 1000)
        {
            @Override
            public void onTick(long millisUntilFinished)
            {
                long day = SDDateUtil.getDuringDay(millisUntilFinished);
                long hour = SDDateUtil.getDuringHours(millisUntilFinished);
                long min = SDDateUtil.getDuringMinutes(millisUntilFinished);
                long sec = SDDateUtil.getDuringSeconds(millisUntilFinished);

                auctionBusinessListener.onAuctionPayRemaining(currentBuyer, day, hour, min, sec);

            }

            @Override
            public void onFinish()
            {

            }
        };
        timer.start();
    }

    /**
     * 停止付款倒计时
     */
    private void stopPayRemaining()
    {
        if (timer != null)
        {
            timer.cancel();
        }
    }

    // 接口

    /**
     * 验证创建竞拍权限
     */
    public void requestCreateAuctionAuthority(AppRequestCallback<BaseActModel> listener)
    {
        AuctionCommonInterface.requestCreateAuctionAuthority(new AppRequestCallbackWrapper<BaseActModel>(listener)
        {
            @Override
            protected void onSuccess(SDResponse sdResponse)
            {
                if (actModel.isOk())
                {
                    auctionBusinessListener.onAuctionRequestCreateAuthoritySuccess();
                } else
                {
                    auctionBusinessListener.onAuctionRequestCreateAuthorityError(actModel.getError());
                }
            }
        });
    }

    /**
     * 请求竞拍信息
     */
    public void requestPaiInfo(int paiId, AppRequestCallback<App_pai_user_get_videoActModel> listener)
    {
        AuctionCommonInterface.requestPaiUserGetVideo(paiId, new AppRequestCallbackWrapper<App_pai_user_get_videoActModel>(listener)
        {
            @Override
            protected void onSuccess(SDResponse sdResponse)
            {
                if (actModel.isOk())
                {
                    boolean auctioning = false;
                    PaiUserGetVideoDataModel data = actModel.getData();
                    if (data != null)
                    {
                        PaiUserGoodsDetailDataInfoModel infoModel = data.getInfo();
                        if (infoModel != null)
                        {
                            if (infoModel.getStatus() == 0)
                            {
                                auctioning = true;
                            }
                        }

                        needShowPay(data.getBuyer());
                    }

                    setAuctioning(auctioning);
                    auctionBusinessListener.onAuctionRequestPaiInfoSuccess(actModel);
                }
            }
        });
    }

    public void clickAuctionPay(View v)
    {
        auctionBusinessListener.onAuctionPayClick(v);
    }

    public void onDestroy()
    {
        stopPayRemaining();
    }

    public interface AuctionBusinessListener
    {
        /**
         * 创建竞拍成功
         */
        void onAuctionMsgCreateSuccess(CustomMsgAuctionCreateSuccess customMsg);

        /**
         * 出价
         */
        void onAuctionMsgOffer(CustomMsgAuctionOffer customMsg);

        /**
         * 竞拍成功
         */
        void onAuctionMsgSuccess(CustomMsgAuctionSuccess customMsg);

        /**
         * 竞拍通知付款，比如第一名超时未付款，通知下一名付款
         */
        void onAuctionMsgNotifyPay(CustomMsgAuctionNotifyPay customMsg);

        /**
         * 竞拍失败
         */
        void onAuctionMsgFail(CustomMsgAuctionFail customMsg);

        /**
         * 支付成功 竞拍结束
         */
        void onAuctionMsgPaySuccess(CustomMsgAuctionPaySuccess customMsg);

        /**
         * 通知付款倒计时
         *
         * @param buyer
         * @param day
         * @param hour
         * @param min
         * @param sec
         */
        void onAuctionPayRemaining(PaiBuyerModel buyer, long day, long hour, long min, long sec);

        /**
         * 本地用户是否显示竞拍支付点击入口
         */
        void onAuctionNeedShowPay(boolean show);

        /**
         * 竞拍支付入口被点击
         */
        void onAuctionPayClick(View v);

        /**
         * 竞拍状态发生变化
         */
        void onAuctioningChange(boolean isAuctioning);

        /**
         * 加载竞拍信息成功
         */
        void onAuctionRequestPaiInfoSuccess(App_pai_user_get_videoActModel actModel);

        /**
         * 验证创建竞拍权限成功
         */
        void onAuctionRequestCreateAuthoritySuccess();

        /**
         * 验证创建竞拍权限失败
         */
        void onAuctionRequestCreateAuthorityError(String msg);
    }

}
