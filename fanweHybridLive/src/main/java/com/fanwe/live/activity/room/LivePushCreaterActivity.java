package com.fanwe.live.activity.room;

import android.content.DialogInterface;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;

import com.fanwe.hybrid.event.EUnLogin;
import com.fanwe.lib.dialog.ISDDialogConfirm;
import com.fanwe.lib.dialog.impl.SDDialogBase;
import com.fanwe.library.receiver.SDNetworkReceiver;
import com.fanwe.library.utils.SDToast;
import com.fanwe.live.LiveInformation;
import com.fanwe.live.R;
import com.fanwe.live.appview.LiveLinkMicGroupView;
import com.fanwe.live.appview.LiveLinkMicView;
import com.fanwe.live.appview.room.ARoomMusicView;
import com.fanwe.live.appview.room.RoomPushMusicView;
import com.fanwe.live.control.IPushSDK;
import com.fanwe.live.control.LivePushSDK;
import com.fanwe.live.dialog.LiveCreaterReceiveApplyLinkMicDialog;
import com.fanwe.live.dialog.LiveSetBeautyDialog;
import com.fanwe.live.dialog.LiveSmallVideoInfoCreaterDialog;
import com.fanwe.live.dialog.common.AppDialogConfirm;
import com.fanwe.live.event.EImOnForceOffline;
import com.fanwe.live.event.EOnCallStateChanged;
import com.fanwe.live.model.App_get_videoActModel;
import com.fanwe.live.model.LiveQualityData;
import com.fanwe.live.model.custommsg.CustomMsgApplyLinkMic;
import com.fanwe.live.model.custommsg.CustomMsgRejectLinkMic;
import com.fanwe.live.model.custommsg.CustomMsgStopLive;
import com.fanwe.live.model.custommsg.data.DataLinkMicInfoModel;
import com.fanwe.live.utils.PermissionUtil;
import com.tencent.TIMCallBack;
import com.tencent.TIMValueCallBack;

/**
 * 推流直播主播界面
 */
public class LivePushCreaterActivity extends LiveLayoutCreaterExtendActivity
{
    private LiveLinkMicGroupView mLinkMicGroupView;

    private boolean mIsCreaterLeaveByCall = false;
    private LiveCreaterReceiveApplyLinkMicDialog mDialogReceiveApplyLinkMic;

    @Override
    protected int onCreateContentView()
    {
        return R.layout.act_live_push_creater;
    }

    @Override
    protected ARoomMusicView createRoomMusicView()
    {
        return new RoomPushMusicView(this);
    }

    @Override
    protected void init(Bundle savedInstanceState)
    {
        super.init(savedInstanceState);
        PermissionUtil.isCameraCanUse();

        if (getRoomId() <= 0)
        {
            SDToast.showToast("房间id为空");
            finish();
            return;
        }

        initPusher();
        initLinkMicGroupView();
        initLayout(getWindow().getDecorView());
        requestRoomInfo();
    }

    @Override
    protected IPushSDK getPushSDK()
    {
        return LivePushSDK.getInstance();
    }

    /**
     * 初始化推流对象
     */
    private void initPusher()
    {
        getPushSDK().init(find(R.id.view_video));
    }

    /**
     * 初始化连麦view
     */
    private void initLinkMicGroupView()
    {
        mLinkMicGroupView = find(R.id.view_link_mic_group);
        mLinkMicGroupView.mCallBack.set(new LiveLinkMicGroupView.LiveLinkMicGroupViewCallback()
        {
            @Override
            public void onPlayDisconnect(String userId)
            {
                getCreaterBusiness().stopLinkMic(userId, false);
            }

            @Override
            public void onPlayRecvFirstFrame(String userId)
            {
                getCreaterBusiness().requestMixStream(userId);
            }

            @Override
            public void onClickView(final LiveLinkMicView view)
            {
                LiveSmallVideoInfoCreaterDialog dialog = new LiveSmallVideoInfoCreaterDialog(LivePushCreaterActivity.this, view.getUserId());
                dialog.setClickListener(new LiveSmallVideoInfoCreaterDialog.ClickListener()
                {
                    @Override
                    public void onClickCloseVideo(View v, String userId)
                    {
                        getCreaterBusiness().stopLinkMic(userId, true);
                    }
                });
                dialog.show();
            }

            @Override
            public void onPushStart(LiveLinkMicView view)
            {

            }
        });
    }

    @Override
    protected void initIM()
    {
        super.initIM();
        if (isClosedBack())
        {
            getGameBusiness().requestGameInfo();
        } else
        {
            if (!TextUtils.isEmpty(getRoomInfo().getGroup_id()))
            {
                final String groupId = getRoomInfo().getGroup_id();
                getCreaterIM().joinGroup(groupId, new TIMCallBack()
                {
                    @Override
                    public void onError(int code, String desc)
                    {
                        dealGroupError(code, desc);
                    }

                    @Override
                    public void onSuccess()
                    {
                        dealGroupSuccess(groupId);
                    }
                });
            } else
            {
                getCreaterIM().createGroup(String.valueOf(getRoomId()), new TIMValueCallBack<String>()
                {
                    @Override
                    public void onError(int code, String desc)
                    {
                        dealGroupError(code, desc);
                    }

                    @Override
                    public void onSuccess(String groupId)
                    {
                        dealGroupSuccess(groupId);
                    }
                });
            }
        }
    }

    /**
     * 加入或者创建聊天组成功处理
     *
     * @param groupId
     */
    private void dealGroupSuccess(String groupId)
    {
        LiveInformation.getInstance().setGroupId(groupId);
        requestUpdateLiveStateSuccess();
    }

    /**
     * 加入或者创建聊天组失败处理
     *
     * @param code
     * @param desc
     */
    protected void dealGroupError(int code, String desc)
    {
        AppDialogConfirm dialog = new AppDialogConfirm(this);
        dialog.setTextContent("创建聊天组失败:" + code + "，请退出重试").setTextCancel(null).setTextConfirm("确定");
        dialog.setCancelable(false);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialog)
            {
                requestUpdateLiveStateFail();
                exitRoom(false);
            }
        });
        dialog.show();
    }


    @Override
    public void onBsRequestRoomInfoSuccess(App_get_videoActModel actModel)
    {
        super.onBsRequestRoomInfoSuccess(actModel);
        if (isClosedBack())
        {
            final String groupId = actModel.getGroup_id();
            requestUpdateLiveStateComeback();
            getCreaterIM().onSuccessJoinGroup(groupId);
            getCreaterIM().sendCreaterComebackMsg(null);
        }

        initIM();
        startPush(actModel.getPush_rtmp());
    }

    @Override
    public void onBsRequestRoomInfoError(App_get_videoActModel actModel)
    {
        super.onBsRequestRoomInfoError(actModel);
        exitRoom(false);
    }

    /**
     * 开始推流
     */
    protected void startPush(String url)
    {
        if (TextUtils.isEmpty(url))
        {
            SDToast.showToast("推流地址为空");
            return;
        }
        if (!isClosedBack())
        {
            addRoomCountDownView();
        }
        getPushSDK().setUrl(url);
        getPushSDK().startPush();

        getPushSDK().enableBeauty(true);
    }

    @Override
    public LiveQualityData onBsGetLiveQualityData()
    {
        return getPushSDK().getLiveQualityData();
    }

    @Override
    public void onBsCreaterShowReceiveApplyLinkMic(final CustomMsgApplyLinkMic msg)
    {
        super.onBsCreaterShowReceiveApplyLinkMic(msg);
        mDialogReceiveApplyLinkMic = new LiveCreaterReceiveApplyLinkMicDialog(this, msg);
        mDialogReceiveApplyLinkMic.setClickListener(new LiveCreaterReceiveApplyLinkMicDialog.ClickListener()
        {
            @Override
            public void onClickAccept()
            {
                getCreaterBusiness().acceptLinkMic(msg.getSender().getUser_id());
            }

            @Override
            public void onClickReject()
            {
                getCreaterBusiness().rejectLinkMic(msg.getSender().getUser_id(), CustomMsgRejectLinkMic.MSG_REJECT);
            }
        });
        mDialogReceiveApplyLinkMic.show();
    }

    @Override
    public void onBsCreaterHideReceiveApplyLinkMic()
    {
        super.onBsCreaterHideReceiveApplyLinkMic();
        if (mDialogReceiveApplyLinkMic != null)
        {
            mDialogReceiveApplyLinkMic.dismiss();
        }
    }

    @Override
    public boolean onBsCreaterIsReceiveApplyLinkMicShow()
    {
        if (mDialogReceiveApplyLinkMic != null)
        {
            return mDialogReceiveApplyLinkMic.isShowing();
        } else
        {
            return false;
        }
    }

    @Override
    public void onMsgDataLinkMicInfo(DataLinkMicInfoModel model)
    {
        super.onMsgDataLinkMicInfo(model);
        boolean hasLinkMicItem = model.hasLinkMicItem();
        boolean needUpdateConfig = false;

        if (hasLinkMicItem)
        {
            if (!getCreaterBusiness().isInLinkMic())
            {
                needUpdateConfig = true;
            }
            mLinkMicGroupView.setLinkMicInfo(model);
            getCreaterBusiness().setLinkMicCount(model.getLinkMicCount());
        } else
        {
            if (getCreaterBusiness().isInLinkMic())
            {
                needUpdateConfig = true;
                mLinkMicGroupView.resetAllView();
            }
        }
        getCreaterBusiness().setInLinkMic(hasLinkMicItem);

        if (needUpdateConfig)
        {
            if (getCreaterBusiness().isInLinkMic())
            {
                getPushSDK().setConfigLinkMicMain();
            } else
            {
                getPushSDK().setConfigDefault();
            }
        }
    }

    @Override
    public void onMsgStopLive(CustomMsgStopLive msg)
    {
        super.onMsgStopLive(msg);
        exitRoom(true);
    }

    /**
     * 退出房间
     *
     * @param addLiveFinish true-显示直播结束界面；false-关闭当前Activity
     */
    protected void exitRoom(boolean addLiveFinish)
    {
        getCreaterBusiness().stopMonitor();
        removeRoomCountDownView();
        getPushSDK().stopPush();
        mLinkMicGroupView.onDestroy();
        stopMusic();

        if (addLiveFinish)
        {
            addLiveFinish();
        } else
        {
            finish();
        }
    }

    @Override
    protected void addLiveFinish()
    {
        super.addLiveFinish();
        getCreaterBusiness().requestEndVideo();
    }

    /**
     * 主播离开
     */
    private void createrLeave()
    {
        if (!mIsCreaterLeave)
        {
            mIsCreaterLeave = true;
            requestUpdateLiveStateLeave();
            getPushSDK().pausePush();
            getCreaterIM().sendCreaterLeaveMsg(null);
            if (getRoomMusicView() != null)
            {
                getRoomMusicView().onStopLifeCircle();
            }

            mLinkMicGroupView.onPause();
        }
    }

    /**
     * 主播回来
     */
    private void createrComeback()
    {
        if (mIsCreaterLeave)
        {
            mIsCreaterLeave = false;
            requestUpdateLiveStateComeback();
            getPushSDK().resumePush();
            getCreaterIM().sendCreaterComebackMsg(null);
            if (getRoomMusicView() != null)
            {
                getRoomMusicView().onResumeLifeCircle();
            }

            mLinkMicGroupView.onResume();
        }
    }


    @Override
    public void onNetworkChanged(SDNetworkReceiver.NetworkType type)
    {
        if (type == SDNetworkReceiver.NetworkType.Mobile)
        {
            AppDialogConfirm dialog = new AppDialogConfirm(this);
            dialog.setTextContent("当前处于数据网络下，会耗费较多流量，是否继续？")
                    .setTextCancel("退出")
                    .setTextConfirm("继续")
                    .setCallback(new ISDDialogConfirm.Callback()
                    {
                        @Override
                        public void onClickCancel(View v, SDDialogBase dialog)
                        {
                            exitRoom(true);
                        }

                        @Override
                        public void onClickConfirm(View v, SDDialogBase dialog)
                        {

                        }
                    }).show();
        }
        super.onNetworkChanged(type);
    }

    @Override
    public void onBackPressed()
    {
        if (isAuctioning())
        {
            showActionExitDialog();
        } else
        {
            showNormalExitDialog();
        }
    }

    @Override
    protected void onClickCloseRoom(View v)
    {
        super.onClickCloseRoom(v);
        if (isAuctioning())
        {
            showActionExitDialog();
        } else
        {
            showNormalExitDialog();
        }
    }

    private void showActionExitDialog()
    {
        AppDialogConfirm dialog = new AppDialogConfirm(this);
        dialog.setTextContent("您发起的竞拍暂未结束，不能关闭直播");
        dialog.setTextConfirm(null);
        dialog.show();
    }

    private void showNormalExitDialog()
    {
        AppDialogConfirm dialog = new AppDialogConfirm(this);
        dialog.setTextContent("确定要结束直播吗？")
                .setCallback(new ISDDialogConfirm.Callback()
                {
                    @Override
                    public void onClickCancel(View v, SDDialogBase dialog)
                    {

                    }

                    @Override
                    public void onClickConfirm(View v, SDDialogBase dialog)
                    {
                        exitRoom(true);
                    }
                }).show();
    }

    private LiveSetBeautyDialog mBeautyDialog;

    @Override
    protected void showSetBeautyDialog()
    {
        if (mBeautyDialog == null)
        {
            mBeautyDialog = new LiveSetBeautyDialog(this);
        }
        mBeautyDialog.showBottom();
    }

    //----------EventBus start----------

    @Override
    public void onEventMainThread(EUnLogin event)
    {
        exitRoom(false);
    }

    @Override
    public void onEventMainThread(EImOnForceOffline event)
    {
        exitRoom(true);
    }

    @Override
    public void onEventMainThread(EOnCallStateChanged event)
    {
        switch (event.state)
        {
            case TelephonyManager.CALL_STATE_RINGING:
            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (mIsCreaterLeave)
                {
                    mIsCreaterLeaveByCall = false;
                } else
                {
                    mIsCreaterLeaveByCall = true;
                }
                createrLeave();
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                if (mIsCreaterLeaveByCall)
                {
                    createrComeback();
                    mIsCreaterLeaveByCall = false;
                }
                break;
            default:
                break;
        }
    }

    //----------EventBus end----------

    //----------activity lifecycle start----------

    @Override
    protected void onResume()
    {
        super.onResume();
        createrComeback();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        createrLeave();
    }

    @Override
    protected void onDestroy()
    {
        getPushSDK().onDestroy();
        mLinkMicGroupView.onDestroy();
        super.onDestroy();
    }

    //----------activity lifecycle end----------

}
