package org.waterforpeople.mapping.app.web.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.waterforpeople.mapping.app.gwt.client.survey.QuestionGroupDto;
import org.waterforpeople.mapping.app.util.DtoMarshaller;
import org.waterforpeople.mapping.app.web.rest.dto.QuestionGroupPayload;

import com.gallatinsystems.common.Constants;
import com.gallatinsystems.survey.dao.QuestionGroupDao;
import com.gallatinsystems.survey.domain.QuestionGroup;


@Controller
@RequestMapping("/question_groups")
public class QuestionGroupRestService {

	@Inject
	private QuestionGroupDao questionGroupDao;

	// TODO put in meta information?
	// list all questionGroups
	@RequestMapping(method = RequestMethod.GET, value = "/all")
	@ResponseBody
	public Map<String, List<QuestionGroupDto>> listQuestionGroups() {
		final Map<String, List<QuestionGroupDto>> response = new HashMap<String, List<QuestionGroupDto>>();
		List<QuestionGroupDto> results = new ArrayList<QuestionGroupDto>();
		List<QuestionGroup> questionGroups = questionGroupDao
				.list(Constants.ALL_RESULTS);
		if (questionGroups != null) {
			for (QuestionGroup s : questionGroups) {
				QuestionGroupDto dto = new QuestionGroupDto();
				DtoMarshaller.copyToDto(s, dto);

				// needed because of different names for description in
				// questionGroup
				// and questionGroupDto
				dto.setDescription(s.getDesc());
				results.add(dto);
			}
		}
		response.put("question_groups", results);
		return response;
	}

	// TODO put in meta information?
	// list questionGroups by survey id
	@RequestMapping(method = RequestMethod.GET, value = "")
	@ResponseBody
	public Map<String, List<QuestionGroupDto>> listQuestionGroupBySurvey(
			@RequestParam("surveyId") Long surveyId) {
		final Map<String, List<QuestionGroupDto>> response = new HashMap<String, List<QuestionGroupDto>>();
		List<QuestionGroupDto> results = new ArrayList<QuestionGroupDto>();
		List<QuestionGroup> questionGroups = questionGroupDao
				.listQuestionGroupBySurvey(surveyId);
		if (questionGroups != null) {
			for (QuestionGroup s : questionGroups) {
				QuestionGroupDto dto = new QuestionGroupDto();
				DtoMarshaller.copyToDto(s, dto);

				// needed because of different names for description in
				// questionGroup
				// and questionGroupDto
				dto.setDescription(s.getDesc());
				results.add(dto);
			}
		}
		response.put("question_groups", results);
		return response;
	}

	// find a single questionGroup by the questionGroupId
	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	@ResponseBody
	public Map<String, QuestionGroupDto> findQuestionGroup(
			@PathVariable("id") Long id) {
		final Map<String, QuestionGroupDto> response = new HashMap<String, QuestionGroupDto>();
		QuestionGroup s = questionGroupDao.getByKey(id);
		QuestionGroupDto dto = null;
		if (s != null) {
			dto = new QuestionGroupDto();
			DtoMarshaller.copyToDto(s, dto);
			// needed because of different names for description in
			// questionGroup and
			// questionGroupDto
			dto.setDescription(s.getDesc());
		}
		response.put("question_group", dto);
		return response;

	}

	// delete questionGroup by id
	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	@ResponseBody
	public Map<String, RestStatusDto> deleteQuestionGroupById(
			@PathVariable("id") Long id) {
		final Map<String, RestStatusDto> response = new HashMap<String, RestStatusDto>();
		QuestionGroup s = questionGroupDao.getByKey(id);
		RestStatusDto statusDto = null;
		statusDto = new RestStatusDto();
		statusDto.setStatus("failed");

		// check if questionGroup exists in the datastore
		if (s != null) {
			// delete questionGroup group
			questionGroupDao.delete(s);
			statusDto.setStatus("ok");
		}
		response.put("meta", statusDto);
		return response;
	}

	// update existing questionGroup
	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	@ResponseBody
	public Map<String, Object> saveExistingQuestionGroup(
			@RequestBody QuestionGroupPayload payLoad) {
		final QuestionGroupDto questionGroupDto = payLoad.getQuestion_group();
		final Map<String, Object> response = new HashMap<String, Object>();
		QuestionGroupDto dto = null;

		RestStatusDto statusDto = new RestStatusDto();
		statusDto.setStatus("failed");

		// if the POST data contains a valid questionGroupDto, continue.
		// Otherwise,
		// server will respond with 400 Bad Request
		if (questionGroupDto != null) {
			Long keyId = questionGroupDto.getKeyId();
			QuestionGroup s;

			// if the questionGroupDto has a key, try to get the questionGroup.
			if (keyId != null) {
				s = questionGroupDao.getByKey(keyId);
				// if we find the questionGroup, update it's properties
				if (s != null) {
					// copy the properties, except the createdDateTime property,
					// because it is set in the Dao.
					BeanUtils.copyProperties(questionGroupDto, s,
							new String[] { "createdDateTime" });
					s = questionGroupDao.save(s);
					dto = new QuestionGroupDto();
					DtoMarshaller.copyToDto(s, dto);
					statusDto.setStatus("ok");
				}
			}
		}
		response.put("meta", statusDto);
		response.put("question_group", dto);
		return response;
	}

	// create new questionGroup
	@RequestMapping(method = RequestMethod.POST, value = "")
	@ResponseBody
	public Map<String, Object> saveNewQuestionGroup(
			@RequestBody QuestionGroupPayload payLoad) {
		final QuestionGroupDto questionGroupDto = payLoad.getQuestion_group();
		final Map<String, Object> response = new HashMap<String, Object>();
		QuestionGroupDto dto = null;

		RestStatusDto statusDto = new RestStatusDto();
		statusDto.setStatus("failed");

		// if the POST data contains a valid questionGroupDto, continue.
		// Otherwise,
		// server will respond with 400 Bad Request
		if (questionGroupDto != null) {
			QuestionGroup s = new QuestionGroup();

			// copy the properties, except the createdDateTime property, because
			// it is set in the Dao.
			BeanUtils.copyProperties(questionGroupDto, s,
					new String[] { "createdDateTime" });
			s = questionGroupDao.save(s);

			dto = new QuestionGroupDto();
			DtoMarshaller.copyToDto(s, dto);
			statusDto.setStatus("ok");
		}

		response.put("meta", statusDto);
		response.put("question_group", dto);
		return response;
	}
}
