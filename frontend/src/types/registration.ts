/*
 * Copyright (c) 2026.
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

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
