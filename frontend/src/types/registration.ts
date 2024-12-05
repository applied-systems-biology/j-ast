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

export class UserRegistrationRequest {
  @Expose()
  email: string = "";

  @Expose()
  password: string = "";

  @Expose()
  firstName: string = "";

  @Expose()
  lastName: string = "";

  @Expose()
  affiliation: string = "";

  @Expose()
  role: UserRole = UserRole.User;
}
