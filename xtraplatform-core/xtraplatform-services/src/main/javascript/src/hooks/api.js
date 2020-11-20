import React, { useCallback, useEffect } from 'react';
import { gql, useQuery, useMutation } from '@apollo/client';
import { useAuth } from '@xtraplatform/manager';

const SERVICES = gql`
    query {
        services @rest(type: "[ServiceStatus]", path: "/services") {
            id
            lastModified
            serviceType
            label
            description
            enabled
            status
            hasBackgroundTask
            progress
            message
        }
    }
`;

const SERVICE_STATUS = gql`
    query($id: String!) {
        status(id: $id) @rest(type: "ServiceStatus", path: "/services/{args.id}/status") {
            id
            lastModified
            serviceType
            label
            description
            enabled
            status
            hasBackgroundTask
            progress
            message
        }
    }
`;

const SERVICE = gql`
    query($id: String!) {
        service(id: $id) @rest(type: "Service", path: "/services/{args.id}") {
            id
            lastModified
            serviceType
            enabled
            secured
            label
            description
            metadata
            api
            collections
        }
    }
`;

const ADD_SERVICE = gql`
    mutation($input: any!) {
        patchService(input: $input)
            @rest(type: "ServiceStatus", path: "/services", method: "POST", bodyKey: "input") {
            id
            lastModified
            serviceType
            label
            description
            enabled
            status
            hasBackgroundTask
            progress
            message
        }
    }
`;

const PATCH_SERVICE = gql`
    mutation($id: String!, $input: any!) {
        patchService(id: $id, input: $input)
            @rest(type: "Service", path: "/services/{args.id}", method: "POST", bodyKey: "input") {
            id
            lastModified
            serviceType
            label
            description
            metadata
            api
            collections
        }
    }
`;

const DELETE_SERVICE = gql`
    mutation($id: String!) {
        deleteService(id: $id)
            @rest(type: "Service", path: "/services/{args.id}", method: "DELETE") {
            NoResponse
        }
    }
`;

const PROVIDER = gql`
    query($id: String!) {
        provider(id: $id) @rest(type: "Provider", path: "/entities/providers/{args.id}") {
            id
            lastModified
            featureProviderType
            connectionInfo
            nativeCrs
            defaultLanguage
            validateTypes
            types
        }
    }
`;

const CODELISTS = gql`
    query {
        codelists @rest(type: "[Codelist]", path: "/entities/codelists") {
            id
        }
    }
`;

const SERVICE_DEFAULTS = gql`
    query($id: String!) {
        defaults(id: $id) @rest(type: "Defaults", path: "/defaults/services/{args.id}") {
            enabled
            secured
            label
            description
            metadata
            api
        }
    }
`;

const PATCH_SERVICE_DEFAULTS = gql`
    mutation($id: String!, $input: any!) {
        defaults(id: $id, input: $input)
            @rest(
                type: "Defaults"
                path: "/defaults/services/{args.id}"
                method: "POST"
                bodyKey: "input"
            ) {
            enabled
            secured
            label
            description
            metadata
            api
        }
    }
`;

const signoutIfNotAuthorized = (result) => {
    const [auth, signin, signout] = useAuth();

    useEffect(() => {
        if (
            result.error &&
            result.error.networkError &&
            result.error.networkError.statusCode === 401 &&
            auth.user
        ) {
            signout(auth.user);
        }
    }, [result, auth, signout]);
};

const useApiQuery = (query, id) => {
    const result = useQuery(query, { variables: { id } });

    signoutIfNotAuthorized(result);

    return result;
};

const useApiMutation = (query, id) => {
    const [doPatch, result] = useMutation(query);

    const patchWrapper = (patch) => {
        //useCallback(
        console.log('CHANGE REQUEST', id, patch);
        return doPatch({ variables: { id, input: patch } })
            .then((result2) => {
                console.log('CHANGE RESPONSE', result2);

                signoutIfNotAuthorized(result2);
            })
            .catch((error) => {
                if (error && error.networkError && error.networkError.statusCode) {
                    console.log('CHANGE ERROR', error.networkError.statusCode);
                }
            });
    };
    //,[doPatch, id]);
    return [patchWrapper, result];
};

export const useServices = () => useApiQuery(SERVICES);
export const useService = (id) => useApiQuery(SERVICE, id);
export const useServiceStatus = (id) => useApiQuery(SERVICE_STATUS, id);
export const useServiceAdd = () => useApiMutation(ADD_SERVICE);
export const useServicePatch = (id) => useApiMutation(PATCH_SERVICE, id);
export const useServiceDelete = (id) => useApiMutation(DELETE_SERVICE, id);
export const useProvider = (id) => useApiQuery(PROVIDER, id);
export const useCodelists = () => useApiQuery(CODELISTS);
export const useServiceDefaults = () => useApiQuery(SERVICE_DEFAULTS, 'ogc_api');
export const useServiceDefaultsPatch = () => useApiMutation(PATCH_SERVICE_DEFAULTS, 'ogc_api');
export const patchDebounce = 2500;
