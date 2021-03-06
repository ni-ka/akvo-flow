import observe from '../../mixins/observe';

FLOW.CurrentDevicesTabView = Ember.View.extend(observe({
  'this.selectedDeviceGroup': 'copyDeviceGroupName',
}), {
  // FLOW.CurrentDevicesTabView = FLOW.View.extend({
  showDeleteDevicesDialogBool: false,
  showAddToGroupDialogBool: false,
  showRemoveFromGroupDialogBool: false,
  showManageDeviceGroupsDialogBool: false,
  newDeviceGroupName: null,
  // bound to devices-list.handlebars
  changedDeviceGroupName: null,
  selectedDeviceGroup: null,
  selectedDeviceGroupForDelete: null,

  // bound to devices-list.handlebars
  showAddToGroupDialog() {
    this.set('selectedDeviceGroup', null);
    this.set('showAddToGroupDialogBool', true);
  },

  showRemoveFromGroupDialog() {
    this.set('showRemoveFromGroupDialogBool', true);
  },

  cancelAddToGroup() {
    this.set('showAddToGroupDialogBool', false);
  },

  showManageDeviceGroupsDialog() {
    this.set('newDeviceGroupName', null);
    this.set('changedDeviceGroupName', null);
    this.set('selectedDeviceGroup', null);
    this.set('showManageDeviceGroupsDialogBool', true);
  },

  cancelManageDeviceGroups() {
    this.set('showManageDeviceGroupsDialogBool', false);
  },

  doAddToGroup() {
    if (this.get('selectedDeviceGroup') !== null) {
      const selectedDeviceGroupId = this.selectedDeviceGroup.get('keyId');
      const selectedDeviceGroupName = this.selectedDeviceGroup.get('code');
      const selectedDevices = FLOW.store.filter(FLOW.Device, (data) => {
        if (data.get('isSelected') === true) {
          return true;
        }
        return false;
      });
      selectedDevices.forEach((item) => {
        item.set('deviceGroupName', selectedDeviceGroupName);
        item.set('deviceGroup', selectedDeviceGroupId);
      });
    }
    FLOW.store.commit();
    this.set('showAddToGroupDialogBool', false);
  },

  // TODO repopulate list after update
  doRemoveFromGroup() {
    const selectedDevices = FLOW.store.filter(FLOW.Device, (data) => {
      if (data.get('isSelected') === true) {
        return true;
      }
      return false;
    });
    selectedDevices.forEach((item) => {
      item.set('deviceGroupName', null);
      item.set('deviceGroup', null);
    });

    FLOW.store.commit();
    this.set('showRemoveFromGroupDialogBool', false);
  },

  cancelRemoveFromGroup() {
    this.set('showRemoveFromGroupDialogBool', false);
  },

  copyDeviceGroupName() {
    if (this.get('selectedDeviceGroup') !== null) {
      this.set('changedDeviceGroupName', this.selectedDeviceGroup.get('code'));
    }
  },

  // TODO update device group name in tabel.
  doManageDeviceGroups() {
    if (this.get('selectedDeviceGroup') !== null) {
      const selectedDeviceGroupId = this.selectedDeviceGroup.get('keyId');

      // this could have been changed in the UI
      const originalSelectedDeviceGroup = FLOW.store.find(FLOW.DeviceGroup, selectedDeviceGroupId);

      if (originalSelectedDeviceGroup.get('code') != this.get('changedDeviceGroupName')) {
        const newName = this.get('changedDeviceGroupName');
        originalSelectedDeviceGroup.set('code', newName);

        const allDevices = FLOW.store.filter(FLOW.Device, () => true);
        allDevices.forEach((item) => {
          if (parseInt(item.get('deviceGroup'), 10) == selectedDeviceGroupId) {
            item.set('deviceGroupName', newName);
          }
        });
      }
    } else if (this.get('newDeviceGroupName') !== null) {
      FLOW.store.createRecord(FLOW.DeviceGroup, {
        code: this.get('newDeviceGroupName'),
      });
    }

    this.set('selectedDeviceGroup', null);
    this.set('newDeviceGroupName', null);
    this.set('changedDeviceGroupName', null);

    FLOW.store.commit();
    this.set('showManageDeviceGroupsDialogBool', false);
  },

  deleteDeviceGroup() {
    const dgroup = this.get('selectedDeviceGroupForDelete');
    if (dgroup !== null) {
      const devicesInGroup = FLOW.store.filter(FLOW.Device, item => item.get('deviceGroup') == dgroup.get('keyId'));
      devicesInGroup.forEach((item) => {
        item.set('deviceGroupName', null);
        item.set('deviceGroup', null);
      });

      FLOW.store.commit();

      dgroup.deleteRecord();
      FLOW.store.commit();
    }
    this.set('showManageDeviceGroupsDialogBool', false);
  },
});


// TODO not used?
FLOW.SavingDeviceGroupView = FLOW.View.extend(observe({
  'FLOW.deviceGroupControl.allRecordsSaved': 'showDGSavingDialog',
}), {
  showDGSavingDialogBool: false,

  showDGSavingDialog() {
    if (FLOW.DeviceGroupControl.get('allRecordsSaved')) {
      this.set('showDGSavingDialogBool', false);
    } else {
      this.set('showDGSavingDialogBool', true);
    }
  },
});
