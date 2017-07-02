import { fork, call, put, take, takeEvery, takeLatest, select } from 'redux-saga/effects'
import ServiceApi from '../apis/ServiceApi'
import normalize from '../apis/ServiceNormalizer'
import { actions, getService } from '../reducers/service'
import { push } from 'react-router-redux'

function* loadServices() {
    try {
        const serviceIds = yield call(ServiceApi.getServices);

        const services = yield serviceIds.map(id => call(ServiceApi.getService, id))

        yield put(actions.fetchedServices(normalize(services)));
    } catch ( error ) {
        yield put(actions.fetchFailed(error));
    }
}

function* loadService(id) {
    try {
        const service = yield call(ServiceApi.getService, id);

        yield put(actions.fetchedService(normalize([service])));
    } catch ( error ) {
        yield put(actions.fetchFailed(error));
    }
}

function* loadServiceConfig(action) {
    try {
        let service = yield select(getService, action.payload)

        if (!service) {
            yield take(actions.fetchedServices);
            service = yield select(getService, action.payload)
        }

        if (!service || (service.status !== 'INITIALIZING' && !service.featureTypes)) {
            //1st step
            const serviceConfig = yield call(ServiceApi.getServiceConfig, action.payload);

            //2nd step
            yield put(actions.fetchedService(normalize([serviceConfig])));
        }
    } catch ( error ) {
        yield put(actions.fetchFailed(error));
    }
}

function* updateServiceConfig(action) {
    try {
        //1st step
        const serviceConfig = yield call(ServiceApi.postServiceConfig, action.payload);
        //console.log(serviceConfig);
        //2nd step
        yield put(actions.fetchedService(normalize([serviceConfig])));
    } catch ( error ) {
        yield put(actions.fetchFailed(error));
    }
}

function* addService(action) {
    try {
        //1st step
        yield put(push('/services/'));
        const serviceConfig = yield call(ServiceApi.postService, action.payload);
        yield call(loadService, action.payload.id);

        //yield take(actions.fetchedServices);

    //2nd step
    //yield put(actions.fetchedService(normalize([serviceConfig])));
    } catch ( error ) {
        yield put(actions.addFailed({
            ...action.payload,
            ...error,
            text: 'Failed to add service with id ' + action.payload.id,
            status: 'critical'
        }));
    }
}

function* removeService(action) {
    try {
        //1st step
        yield put(push('/services/'));
        const serviceConfig = yield call(ServiceApi.deleteService, action.payload.id);

        //yield take(actions.fetchedServices);

    //2nd step
    //yield put(actions.fetchedService(normalize([serviceConfig])));
    } catch ( error ) {
        yield put(actions.fetchFailed(error));
    }
}

function* rootSaga() {
    //yield fork(loadServices);
    yield takeEvery(actions.selectService, loadServiceConfig);
    yield takeEvery(actions.updateService, updateServiceConfig);
    yield takeEvery(actions.addService, addService);
    yield takeEvery(actions.removeService, removeService);
}

export default rootSaga;
