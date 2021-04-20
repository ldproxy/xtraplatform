import React, { useEffect } from 'react';
import { gql, useQuery, useMutation } from '@apollo/client';
import { useAuth } from './auth';

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

export const useApiQuery = (query, id) => {
    const result = useQuery(query, { variables: { id } });

    signoutIfNotAuthorized(result);

    return result;
};

export const useApiMutation = (query, id, options = {}) => {
    const [doPatch, result] = useMutation(query, options);

    const patchWrapper = (patch) => {
        //useCallback(
        console.log('CHANGE REQUEST', id, patch, options);
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

const CHANGE_PASSWORD = gql`
    mutation($id: String!, $input: any!) {
        patchService(id: $id, input: $input)
            @rest(type: "User", path: "/users/{args.id}", method: "POST", bodyKey: "input") {
            id
            createtAt
            lastModified
            role
        }
    }
`;

export const useChangePassword = (id) => useApiMutation(CHANGE_PASSWORD, id);
export const patchDebounce = 2500;
