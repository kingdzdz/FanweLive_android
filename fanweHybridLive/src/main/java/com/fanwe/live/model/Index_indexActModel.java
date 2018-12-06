package com.fanwe.live.model;

import java.util.List;

import com.fanwe.hybrid.model.BaseActListModel;

public class Index_indexActModel extends BaseActListModel
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private List<LiveRoomModel> list;
	private List<LiveBannerModel> banner;
	private LiveTopicInfoModel cate;

	public List<LiveRoomModel> getList()
	{
		return list;
	}

	public void setList(List<LiveRoomModel> list)
	{
		this.list = list;
	}

	public List<LiveBannerModel> getBanner()
	{
		return banner;
	}

	public void setBanner(List<LiveBannerModel> banner)
	{
		this.banner = banner;
	}

	public LiveTopicInfoModel getCate()
	{
		return cate;
	}

	public void setCate(LiveTopicInfoModel cate)
	{
		this.cate = cate;
	}

}
