import {userErrorMessage} from '../../../shared/labels/user-error-message';

export function accessErrorMessage(error: unknown): string {
  return userErrorMessage(error);
}
