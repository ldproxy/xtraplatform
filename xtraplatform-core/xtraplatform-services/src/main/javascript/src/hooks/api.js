import React from 'react';
import { useApolloClient, gql } from '@apollo/client';
import { useApiQuery, useApiMutation } from '@xtraplatform/manager';

const SERVICES = gql`
    query {
        services @rest(type: "[ServiceStatus]", path: "/services") {
            id
            createtAt
            lastModified
            serviceType
            label
            description
            enabled
            status
            hasBackgroundTask
            hasProgress
            progress
            message
        }
    }
`;

const SERVICE_STATUS = gql`
    query ($id: String!) {
        status(id: $id) @rest(type: "ServiceStatus", path: "/services/{args.id}/status") {
            id
            createtAt
            lastModified
            serviceType
            label
            description
            enabled
            status
            hasBackgroundTask
            hasProgress
            progress
            message
        }
    }
`;

const SERVICE = gql`
    query ($id: String!) {
        service(id: $id) @rest(type: "Service", path: "/services/{args.id}") {
            id
            createtAt
            lastModified
            serviceType
            enabled
            secured
            label
            description
            apiVersion
            metadata
            defaultExtent
            api
            collections
        }
    }
`;

const ADD_SERVICE = gql`
    mutation ($input: any!) {
        patchService(input: $input)
            @rest(type: "ServiceStatus", path: "/services", method: "POST", bodyKey: "input") {
            id
            createtAt
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
    mutation ($id: String!, $input: any!) {
        patchService(id: $id, input: $input)
            @rest(type: "Service", path: "/services/{args.id}", method: "POST", bodyKey: "input") {
            id
            createtAt
            lastModified
            serviceType
            label
            description
            apiVersion
            metadata
            defaultExtent
            api
            collections
        }
    }
`;

const DELETE_SERVICE = gql`
    mutation ($id: String!) {
        deleteService(id: $id)
            @rest(type: "Service", path: "/services/{args.id}", method: "DELETE") {
            NoResponse
        }
    }
`;

const PROVIDER = gql`
    query ($id: String!) {
        provider(id: $id) @rest(type: "Provider", path: "/entities/providers/{args.id}") {
            id
            lastModified
            providerSubType
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

const CODELIST = gql`
    query ($id: String!) {
        codelist(id: $id) @rest(type: "Codelist", path: "/entities/codelists/{args.id}") {
            id
            createtAt
            lastModified
            label
            entries
        }
    }
`;

const SERVICE_DEFAULTS = gql`
    query ($id: String!) {
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
    mutation ($id: String!, $input: any!) {
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

export const useServices = () => useApiQuery(SERVICES);
export const useService = (id) => useApiQuery(SERVICE, id);
export const useServiceStatus = (id) => useApiQuery(SERVICE_STATUS, id);
export const useServiceAdd = () => useApiMutation(ADD_SERVICE);
export const useServicePatch = (id, options) => useApiMutation(PATCH_SERVICE, id, options);
export const useServiceDelete = (id) => useApiMutation(DELETE_SERVICE, id);
export const useProvider = (id) => useApiQuery(PROVIDER, id);
export const useCodelists = () => useApiQuery(CODELISTS);
export const useCodelist = (id) => useApiQuery(CODELIST, id);
export const useServiceDefaults = () => useApiQuery(SERVICE_DEFAULTS, 'ogc_api');
export const useServiceDefaultsPatch = () => useApiMutation(PATCH_SERVICE_DEFAULTS, 'ogc_api');
export const useInvalidateCache = () => {
    const client = useApolloClient();
    return () => client.resetStore();
};
export const patchDebounce = 2500;
