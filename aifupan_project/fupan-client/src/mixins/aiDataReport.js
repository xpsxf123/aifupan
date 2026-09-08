export default {
    data() {
        return {
            tabsList: [],
            selectedCueWord: [],
            selectedRow: {},
            isReport: false,
            drawerStatus: false,
            sponsorship: ''
        };
    },
    methods: {
        hideReportModal(status) {
            this.isReport = status
        },
        async getListDiagnosis(isAddCompere) {
            return []
        }
    }
}