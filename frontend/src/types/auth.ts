import { Expose } from 'class-transformer';
import { UserRole } from 'src/types/registration';

export class UserAuthenticationLoginResponse {
  @Expose()
  accessToken: string = '';

  @Expose()
  refreshToken: string = '';

  @Expose()
  username: string = '';

  @Expose()
  role: UserRole = UserRole.Guest;

  @Expose()
  authorities: string[] = [];

  @Expose()
  guestExpireSeconds: number = 0;

  @Expose()
  guestMaxProjects: number = 0;

  @Expose()
  guestMaxImages: number = 0;

  @Expose()
  guestMaxExpireSeconds: number = 0;

  toLimits() {
    const result = new GuestLimits();
    result.guestExpireSeconds = this.guestExpireSeconds;
    result.guestMaxExpireSeconds = this.guestMaxExpireSeconds;
    result.guestMaxProjects = this.guestMaxProjects;
    result.guestMaxImages = this.guestMaxImages;
    return result;
  }
}

export class GuestLimits {
  @Expose()
  guestExpireSeconds: number = 0;

  @Expose()
  guestMaxProjects: number = 0;

  @Expose()
  guestMaxImages: number = 0;

  @Expose()
  guestMaxExpireSeconds: number = 0;
}
