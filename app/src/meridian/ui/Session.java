package meridian.ui;

import meridian.data.User;

/** The logged-on user. The company code is asked once per session, on the first FB60 call. */
final class Session {
    final User user;
    String companyCode;

    Session(User user) {
        this.user = user;
    }
}
