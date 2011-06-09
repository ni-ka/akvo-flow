package org.waterforpeople.mapping.app.gwt.server.survey;

import java.util.ArrayList;
import java.util.List;

import org.waterforpeople.mapping.app.gwt.client.survey.MetricDto;
import org.waterforpeople.mapping.app.gwt.client.survey.MetricService;
import org.waterforpeople.mapping.app.util.DtoMarshaller;

import com.gallatinsystems.framework.gwt.dto.client.ResponseDto;
import com.gallatinsystems.metric.dao.MetricDao;
import com.gallatinsystems.metric.domain.Metric;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class MetricServiceImpl extends RemoteServiceServlet implements
		MetricService {

	private static final long serialVersionUID = -7385390184438218799L;
	private MetricDao metricDao;

	public MetricServiceImpl() {
		metricDao = new MetricDao();
	}

	/**
	 * lists all metrics, optionally filtered by organization, valueType and
	 * name and group
	 * 
	 * 
	 * @param organizationName
	 * @return
	 */
	@Override
	public ResponseDto<ArrayList<MetricDto>> listMetrics(String name,
			String group, String valueType, String organizationName,
			String cursor) {
		List<Metric> metrics = metricDao.listMetrics(name, group, valueType,
				organizationName, cursor);
		ResponseDto<ArrayList<MetricDto>> resp = new ResponseDto<ArrayList<MetricDto>>();
		ArrayList<MetricDto> dtoList = new ArrayList<MetricDto>();
		if (metrics != null) {
			for (Metric m : metrics) {
				MetricDto dto = new MetricDto();
				DtoMarshaller.copyToDto(m, dto);
				dtoList.add(dto);
			}
			resp.setCursorString(MetricDao.getCursor(metrics));
		}
		resp.setPayload(dtoList);
		return resp;
	}

	/**
	 * deletes the metric with the given id. this method does NOT remove orphans
	 */
	@Override
	public void deleteMetric(Long id) {
		Metric m = metricDao.getByKey(id);
		if (m != null) {
			metricDao.delete(m);
		}
	}

	/**
	 * saves or updates a metric. If the metric passed in does not have a key,
	 * this method will first check for duplicates before saving. if the metric
	 * already exists, the existing metric will be returned
	 */
	@Override
	public MetricDto saveMetric(MetricDto metric) {
		Metric mToSave = new Metric();
		DtoMarshaller.copyToCanonical(mToSave, metric);
		if (metric.getKeyId() == null) {
			List<Metric> mList = metricDao.listMetrics(metric.getName(),
					metric.getGroup(), metric.getValueType(),
					metric.getOrganization(), null);
			if (mList != null && mList.size() > 0) {
				DtoMarshaller.copyToDto(mList.get(0), metric);
				return metric;
			}
		}
		// if we get here, we need to perform the save
		mToSave = metricDao.save(mToSave);
		metric.setKeyId(mToSave.getKey().getId());
		return metric;
	}

}