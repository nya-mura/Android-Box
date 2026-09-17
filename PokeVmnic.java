import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import java.lang.reflect.Method;

public class PokeVmnic {
    private static final String SERVICE_NAME =
        "android.system.virtualizationservice_internal.IVmnic";
    private static final String INTERFACE_DESCRIPTOR =
        "android.system.virtualizationservice_internal.IVmnic";

    // createTapInterface is the first method declared in the AIDL interface
    // (per aidl.rs impl order), so its transaction code should be
    // FIRST_CALL_TRANSACTION + 0. deleteTapInterface would be +1.
    private static final int TRANSACTION_createTapInterface =
        IBinder.FIRST_CALL_TRANSACTION + 0;

    public static void main(String[] args) {
        String suffix = args.length > 0 ? args[0] : "manual0";

        try {
            Method getService = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String.class);
            IBinder binder = (IBinder) getService.invoke(null, SERVICE_NAME);

            if (binder == null) {
                System.out.println("[-] getService() returned null.");
                System.out.println("    Either the service name is wrong, or SELinux");
                System.out.println("    is hiding it from this process's domain.");
                return;
            }

            System.out.println("[+] Got binder: " + binder
                + "  isBinderAlive=" + binder.isBinderAlive());

            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(INTERFACE_DESCRIPTOR);
                data.writeString(suffix);

                boolean ok = binder.transact(TRANSACTION_createTapInterface, data, reply, 0);
                System.out.println("[+] transact() returned: " + ok);

                // Throws a RuntimeException with the service-specific exception
                // message if the remote side returned an error status.
                reply.readException();

                boolean hasResult = reply.readInt() != 0;
                if (hasResult) {
                    ParcelFileDescriptor pfd =
                        ParcelFileDescriptor.CREATOR.createFromParcel(reply);
                    System.out.println("[+] Success. Tap fd = " + pfd.getFd()
                        + "  (interface should be named avf_tap_" + suffix + ")");
                    System.out.println("    Note: this fd only exists in THIS process.");
                    System.out.println("    Getting it into crosvm's process needs a");
                    System.out.println("    separate fd-passing step (e.g. SCM_RIGHTS),");
                    System.out.println("    or you check `ip link` on the host directly");
                    System.out.println("    to confirm avf_tap_" + suffix + " now exists.");
                } else {
                    System.out.println("[-] Reply had no result (unexpected).");
                }
            } finally {
                data.recycle();
                reply.recycle();
            }
        } catch (Exception e) {
            System.out.println("[-] Failed:");
            e.printStackTrace();
        }
    }
}
