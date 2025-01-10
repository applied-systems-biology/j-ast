import { Expose } from 'class-transformer';

export enum UserRole {
  User = 'User',
  Guest = 'Guest',
  Admin = 'Admin',
}

export class UserRegistrationAllowedFeaturesPayload {
  @Expose()
  allowSelfRegister: boolean = false;

  @Expose()
  allowGuestAccounts: boolean = true;

  @Expose()
  guestProjectLimit: number = 1;

  @Expose()
  guestImageLimit: number = 10;

  @Expose()
  guestAccountExpireMinutes: number = 60 * 24 * 3;

  @Expose()
  adminContact: string = "<Not provided>";
}

export class UserPayload {
  @Expose()
  email: string = "";

  @Expose()
  newPassword: string = "";

  @Expose()
  newPasswordConfirm: string = "";

  @Expose()
  firstName: string = "";

  @Expose()
  lastName: string = "";

  @Expose()
  affiliation: string = "";

  @Expose()
  role: UserRole = UserRole.User;

  @Expose()
  id: number = -1;

  @Expose()
  allowLogin: boolean = true;
}
