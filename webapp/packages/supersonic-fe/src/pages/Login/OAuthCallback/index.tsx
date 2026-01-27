import React, { useEffect, useState } from 'react';
import { Spin, Result, Button } from 'antd';
import { history, useModel } from '@umijs/max';
import { AUTH_TOKEN_KEY, TENANT_ID_KEY } from '@/common/constants';
import { queryCurrentUser } from '@/services/user';
import { ROUTE_AUTH_CODES } from '../../../../config/routes';
import { exchangeOAuthCode } from '../services';
import styles from './style.less';

/**
 * OAuth callback page. This page is redirected to after OAuth provider authentication.
 * It exchanges the one-time code (stored in HTTP-only cookie) for access tokens.
 */
const OAuthCallback: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { initialState = {}, setInitialState } = useModel('@@initialState');

  useEffect(() => {
    handleOAuthCallback();
  }, []);

  const handleOAuthCallback = async () => {
    try {
      // Check for error in URL params
      const urlParams = new URLSearchParams(window.location.hash.split('?')[1] || '');
      const errorParam = urlParams.get('error');
      if (errorParam) {
        setError(getErrorMessage(errorParam));
        setLoading(false);
        return;
      }

      // Exchange the code for tokens
      const result = await exchangeOAuthCode();

      if (result.error) {
        setError(getErrorMessage(result.error));
        setLoading(false);
        return;
      }

      // Store the access token
      if (result.access_token) {
        localStorage.setItem(AUTH_TOKEN_KEY, result.access_token);

        // Store refresh token if provided
        if (result.refresh_token) {
          localStorage.setItem('SUPERSONIC_REFRESH_TOKEN', result.refresh_token);
        }

        // Store session ID if provided
        if (result.session_id) {
          localStorage.setItem('SUPERSONIC_SESSION_ID', result.session_id);
        }

        // Fetch current user info
        const { code: queryUserCode, data: queryUserData } = await queryCurrentUser();
        if (queryUserCode === 200) {
          const currentUser = {
            ...queryUserData,
            staffName: queryUserData.staffName || queryUserData.name,
          };
          const authCodes = Array.isArray(initialState?.authCodes) ? [...initialState.authCodes] : [];
          if (queryUserData.superAdmin) {
            authCodes.push(ROUTE_AUTH_CODES.SYSTEM_ADMIN);
          }
          setInitialState({ ...initialState, currentUser, authCodes });

          // Store tenant ID for multi-tenancy support
          if (queryUserData.tenantId) {
            localStorage.setItem(TENANT_ID_KEY, String(queryUserData.tenantId));
          }
        }

        // Redirect to home page
        history.push('/');
      } else {
        setError('Failed to retrieve access token');
        setLoading(false);
      }
    } catch (err: any) {
      console.error('OAuth callback error:', err);
      setError(err.message || 'An unexpected error occurred');
      setLoading(false);
    }
  };

  const getErrorMessage = (errorCode: string): string => {
    const errorMessages: Record<string, string> = {
      invalid_request: 'Invalid request. Please try again.',
      invalid_code: 'Authentication code has expired. Please try again.',
      exchange_failed: 'Failed to exchange authentication code.',
      access_denied: 'Access was denied by the OAuth provider.',
      invalid_state: 'Invalid state parameter. Please try again.',
      missing_params: 'Missing required parameters.',
      network_error: 'Network error. Please check your connection.',
    };
    return errorMessages[errorCode] || `Authentication failed: ${errorCode}`;
  };

  const handleRetry = () => {
    history.push('/login');
  };

  if (loading) {
    return (
      <div className={styles.callbackContainer}>
        <Spin size="large" tip="Completing authentication..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className={styles.callbackContainer}>
        <Result
          status="error"
          title="Authentication Failed"
          subTitle={error}
          extra={[
            <Button type="primary" key="retry" onClick={handleRetry}>
              Back to Login
            </Button>,
          ]}
        />
      </div>
    );
  }

  return (
    <div className={styles.callbackContainer}>
      <Spin size="large" tip="Redirecting..." />
    </div>
  );
};

export default OAuthCallback;
