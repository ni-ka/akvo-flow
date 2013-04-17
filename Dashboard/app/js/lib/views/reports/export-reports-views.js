/*global Ember, $, FLOW */

FLOW.ReportLoader = Ember.Object.create({
    criteria: null,
    timeout: 6000,
    requestInterval: 3000,

    payloads: {
        RAW_DATA: {
            surveyId: '75201',
            exportType: 'RAW_DATA',
            opts: {
                locale: 'en',
                exportMode: 'RAW_DATA',
                generateTabFormat: 'false'
            }
        },
        GRAPHICAL_SURVEY_SUMMARY: {
            surveyId: '75201',
            exportType: 'GRAPHICAL_SURVEY_SUMMARY',
            opts: {
                locale: 'en',
                performRollup: 'false',
                nocharts: 'true'
            }
        },
        SURVEY_FORM: {
            surveyId: '75201',
            exportType: 'SURVEY_FORM',
            opts: {}
        }
    },

    load: function (exportType, surveyId, opts) {
        var criteria;

        if (this.get('criteria')) {
          return;
        }

        Ember.assert('exportType param is required', exportType !== undefined);
        Ember.assert('surveyId param is required', surveyId !== undefined);

        criteria = Ember.copy(this.get('payloads')[exportType]);
        criteria.surveyId = '' + surveyId;
        criteria.baseURL = location.protocol + '//' + location.host;
        criteria.opts.imagePrefix = FLOW.Env.photo_url_root;

        if (opts) {
            Ember.keys(opts).forEach(function (k) {
                criteria.opts[k] = opts[k];
            });
        }

        this.set('criteria', criteria);
        this.requestReport();
    },

    handleResponse: function (resp) {
        if (!resp || resp.status !== 'OK') {
          this.showError();
          return;
        }

        if (resp.message === 'PROCESSING') {
          this.set('processing', false);
          Ember.run.later(this, this.requestReport, this.requestInterval);
        } else if (resp.file) {
          FLOW.savingMessageControl.set('areLoadingBool', false);
          this.set('processing', false);
          this.set('criteria', null);
          $('#downloader').attr('src',  FLOW.Env.reportService +'/report/' + resp.file);
        }
    },

    requestReport: function () {
        FLOW.savingMessageControl.set('areLoadingBool', true);
        this.set('processing', true);
        $.ajax({
            url: FLOW.Env.reportService + '/generate',
            data: {
                criteria: JSON.stringify(this.get('criteria'))
            },
            jsonpCallback: 'FLOW.ReportLoader.handleResponse',
            dataType: 'jsonp'
        });

        Ember.run.later(this, this.handleError, this.timeout);
    },

    handleError: function () {
      if(this.get('processing')) {
        this.showError();
      }
    },

    showError: function () {
      FLOW.savingMessageControl.set('areLoadingBool', false);
      this.set('processing', false);
      this.set('criteria', null);
      FLOW.dialogControl.set('activeAction', 'ignore');
      FLOW.dialogControl.set('header', Ember.String.loc('_error_generating_report'));
      FLOW.dialogControl.set('message', Ember.String.loc('_error_generating_report_try_later'));
      FLOW.dialogControl.set('showCANCEL', false);
      FLOW.dialogControl.set('showDialog', true);
    }
});

FLOW.ExportReportsAppletView = FLOW.View.extend({
    showRawDataReportApplet: false,
    showComprehensiveReportApplet: false,
    showGoogleEarthFileApplet: false,
    showSurveyFormApplet: false,
    showComprehensiveDialog: false,
    showRawDataImportApplet: false,
    showGoogleEarthButton: false,

    showRawDataReport: function () {
      if (!FLOW.selectedControl.selectedSurvey) {
        this.showWarning();
        return;
      }
      FLOW.ReportLoader.load('RAW_DATA', FLOW.selectedControl.selectedSurvey.get('id'));
    },

    showComprehensiveReport: function () {
        var opts = {};
        this.set('showComprehensiveDialog', false);

        opts.performRollup = '' + FLOW.editControl.summaryPerGeoArea;
        opts.nocharts = '' + FLOW.editControl.omitCharts;

        FLOW.ReportLoader.load('GRAPHICAL_SURVEY_SUMMARY', FLOW.selectedControl.selectedSurvey.get('id'), opts);
    },

    showGoogleEarthFile: function () {
      if (!FLOW.selectedControl.selectedSurvey) {
        this.showWarning();
        return;
      }
      this.renderApplet('showGoogleEarthFileApplet', true);
    },

    showSurveyForm: function () {
      if (!FLOW.selectedControl.selectedSurvey) {
        this.showWarning();
        return;
      }
      FLOW.ReportLoader.load('SURVEY_FORM', FLOW.selectedControl.selectedSurvey.get('id'));
    },

    showImportApplet: function () {
        this.renderApplet('showRawDataImportApplet', true);
    },

    showComprehensiveOptions: function () {
        if (!FLOW.selectedControl.selectedSurvey) {
            this.showWarning();
            return;
        }

        FLOW.editControl.set('summaryPerGeoArea', true);
        FLOW.editControl.set('omitCharts', false);
        this.set('showComprehensiveDialog', true);
    },

    showWarning: function () {
        FLOW.dialogControl.set('activeAction', 'ignore');
        FLOW.dialogControl.set('header', Ember.String.loc('_export_data'));
        FLOW.dialogControl.set('message', Ember.String.loc('_applet_select_survey'));
        FLOW.dialogControl.set('showCANCEL', false);
        FLOW.dialogControl.set('showDialog', true);
    },

    renderApplet: function (prop, skipSurveyCheck) {
        if (!skipSurveyCheck && !FLOW.selectedControl.selectedSurvey) {
            this.showWarning();
            return;
        }

        if (prop === 'showRawDataImportApplet') {
          this.set('showRawDataReportApplet', false);
          this.set('showComprehensiveReportApplet', false);
          this.set('showGoogleEarthFileApplet', false);
          this.set('showSurveyFormApplet', true);
          this.set('showRawDataImportApplet', true);
        }

        this.get('childViews').forEach(function (v) {
            if (v.get('childViews') && v.get('childViews').length > 0) {
                return; // skip initial select items
            }

            if (v.state === 'inDOM') {
                v.rerender();
            }
        });
    }
});
