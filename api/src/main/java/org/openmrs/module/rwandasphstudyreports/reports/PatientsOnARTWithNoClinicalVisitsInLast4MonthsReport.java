package org.openmrs.module.rwandasphstudyreports.reports;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.common.SortCriteria;
import org.openmrs.module.reporting.common.SortCriteria.SortDirection;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateDiff;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateDiff.DateDiffType;
import org.openmrs.module.rwandareports.reporting.SetupReport;
import org.openmrs.module.rwandasphstudyreports.Cohorts;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertiesManagement;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertyConstants;
import org.openmrs.module.rwandasphstudyreports.Helper;
import org.openmrs.module.rwandasphstudyreports.RowPerPatientColumns;

public class PatientsOnARTWithNoClinicalVisitsInLast4MonthsReport implements SetupReport {
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();

	private Program hivProgram;

	private Concept scheduledVisit;

	private List<EncounterType> encounterTypes;

	private Concept cd4Count;

	private Concept viralLoad;

	private EncounterType adultFollowUpEncounterType;

	private Concept hivStatus;

	private Concept telephone;

	private Concept telephone2;

	private Concept guardianTelephone;

	private Concept contactTelephone;

	@Override
	public void setup() throws Exception {
		setupProperties();

		ReportDefinition rd = createReportDefinition();
		ReportDesign design = Helper.createRowPerPatientXlsOverviewReportDesign(rd,
				"PatientsOnARTWithNoClinicalVisitsInLast4Months.xls", "PatientsOnARTWithNoClinicalVisitsInLast4Months",
				null);
		Properties props = new Properties();

		props.put("repeatingSections", "sheet:1,row:6,dataset:PatientsOnARTWithNoClinicalVisitsInLast4Months");
		props.put("sortWeight", "5000");
		design.setProperties(props);

		Helper.saveReportDesign(design);
	}

	@Override
	public void delete() {
		ReportService rs = Context.getService(ReportService.class);

		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("PatientsOnARTWithNoClinicalVisitsInLast4Months".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}
		Helper.purgeReportDefinition("PatientsOnARTWithNoClinicalVisitsInLast4Months");
	}

	private ReportDefinition createReportDefinition() {
		ReportDefinition reportDefinition = new ReportDefinition();

		reportDefinition.setName("PatientsOnARTWithNoClinicalVisitsInLast4Months");
		reportDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		reportDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		reportDefinition.addParameter(new Parameter("location", "Health Center", Location.class));
		reportDefinition.setBaseCohortDefinition(Cohorts.createParameterizedLocationCohort("At Location"),
				ParameterizableUtil.createParameterMappings("location=${location}"));

		createDataSetDefinition(reportDefinition);
		Helper.saveReportDefinition(reportDefinition);

		return reportDefinition;
	}

	private void createDataSetDefinition(ReportDefinition reportDefinition) {
		RowPerPatientDataSetDefinition dataSetDefinition = new RowPerPatientDataSetDefinition();
		DateDiff monthSinceLastVisit = RowPerPatientColumns.getDifferenceSinceLastEncounter("MonthsSinceLastVisit",
				encounterTypes, DateDiffType.MONTHS);
		DateDiff monthSinceLastCD4 = RowPerPatientColumns.getDifferenceSinceLastObservation("MonthsSinceLastCD4",
				cd4Count, DateDiffType.MONTHS);
		DateDiff monthSinceLastVL = RowPerPatientColumns.getDifferenceSinceLastObservation("MonthsSinceLastViralLoad",
				viralLoad, DateDiffType.MONTHS);
		SortCriteria sortCriteria = new SortCriteria();
		Map<String, Object> mappings = new HashMap<String, Object>();

		mappings.put("endDate", "${endDate}");
		mappings.put("startDate", "${startDate}");

		sortCriteria.addSortElement("nextRDV", SortDirection.ASC);
		sortCriteria.addSortElement("familyName", SortDirection.ASC);
		sortCriteria.addSortElement("LastVisit Date", SortDirection.DESC);
		dataSetDefinition.setSortCriteria(sortCriteria);

		dataSetDefinition.addParameter(reportDefinition.getParameter("startDate"));
		dataSetDefinition.addParameter(reportDefinition.getParameter("endDate"));
		dataSetDefinition.setName(reportDefinition.getName() + " Data Set");
		dataSetDefinition.addFilter(Cohorts.createInProgramParameterizableByDate("adultHIV: In Program", hivProgram),
				ParameterizableUtil.createParameterMappings("onDate=${now}"));

		dataSetDefinition.addColumn(RowPerPatientColumns.getTracnetId("TRACNET_ID"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getGender("sex"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getDateOfBirth("birth_date", "dd/MMM/yyyy", null),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentCD4("cD4Test", "dd/MMM/yyyy"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentViralLoad("viralLoad", "dd/MMM/yyyy"),
				new HashMap<String, Object>());

		monthSinceLastVisit.addParameter(reportDefinition.getParameter("endDate"));
		monthSinceLastCD4.addParameter(reportDefinition.getParameter("endDate"));
		monthSinceLastVL.addParameter(reportDefinition.getParameter("endDate"));
		monthSinceLastVisit.addParameter(reportDefinition.getParameter("startDate"));
		monthSinceLastCD4.addParameter(reportDefinition.getParameter("startDate"));
		monthSinceLastVL.addParameter(reportDefinition.getParameter("startDate"));

		dataSetDefinition.addColumn(monthSinceLastVisit,
				ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}"));
		dataSetDefinition.addColumn(monthSinceLastCD4,
				ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}"));
		dataSetDefinition.addColumn(monthSinceLastVL,
				ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}"));
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("nextRDV", scheduledVisit, "dd/MMM/yyyy"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("telephone", telephone, null),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("telephone2", telephone2, null),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getPatientAddress("address", true, true, true, true),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getAccompRelationship("accompagnateur"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("contactTel", contactTelephone, null),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("guardianTel", guardianTelephone, null),
				new HashMap<String, Object>());

		SqlCohortDefinition adultPatientsCohort = Cohorts.getAdultPatients();
		SqlCohortDefinition onART = Cohorts.getPatientsOnOrNotOnART(true);
		SqlCohortDefinition withVisits = Cohorts.patientsWithNoClinicalVisitforMoreThanNMonths(4);

		dataSetDefinition.addFilter(adultPatientsCohort, null);
		dataSetDefinition.addFilter(onART, null);
		dataSetDefinition.addFilter(withVisits, null);

		reportDefinition.addDataSetDefinition("PatientsOnARTWithNoClinicalVisitsInLast4Months", dataSetDefinition,
				mappings);
	}

	private void setupProperties() {
		hivProgram = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
		scheduledVisit = gp.getConcept(GlobalPropertyConstants.RETURN_VISIT_CONCEPTID);
		encounterTypes = gp.getEncounterTypeList(GlobalPropertyConstants.ADULT_ENCOUNTER_TYPE_IDS);
		cd4Count = gp.getConcept(GlobalPropertyConstants.CD4_COUNT_CONCEPTID);
		viralLoad = gp.getConcept(GlobalPropertyConstants.VIRAL_LOAD_CONCEPTID);
		adultFollowUpEncounterType = gp.getEncounterType(GlobalPropertyConstants.ADULT_FOLLOWUP_ENCOUNTER_TYPEID);
		hivStatus = gp.getConcept(GlobalPropertyConstants.HIV_STATUS_CONCEPTID);
		telephone = gp.getConcept(GlobalPropertiesManagement.TELEPHONE_NUMBER_CONCEPT);
		telephone2 = gp.getConcept(GlobalPropertiesManagement.SECONDARY_TELEPHONE_NUMBER_CONCEPT);
		contactTelephone = gp.getConcept(GlobalPropertyConstants.CONTACT_TEL_CONCEPTID);
		guardianTelephone = gp.getConcept(GlobalPropertyConstants.GUARDIAN_TEL_CONCEPTID);
	}
}