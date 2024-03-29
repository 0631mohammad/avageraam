/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Grishka, 2013-2016.
 */

package ir.avageram.com.voip;

import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.SystemClock;

import ir.avageram.com.ApplicationLoader;
import ir.avageram.com.BuildConfig;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;

public class VoIPController {

	public static final int NET_TYPE_UNKNOWN = 0;
	public static final int NET_TYPE_GPRS = 1;
	public static final int NET_TYPE_EDGE = 2;
	public static final int NET_TYPE_3G = 3;
	public static final int NET_TYPE_HSPA = 4;
	public static final int NET_TYPE_LTE = 5;
	public static final int NET_TYPE_WIFI = 6;
	public static final int NET_TYPE_ETHERNET = 7;
	public static final int NET_TYPE_OTHER_HIGH_SPEED = 8;
	public static final int NET_TYPE_OTHER_LOW_SPEED = 9;
	public static final int NET_TYPE_DIALUP = 10;
	public static final int NET_TYPE_OTHER_MOBILE = 11;

	public static final int STATE_WAIT_INIT = 1;
	public static final int STATE_WAIT_INIT_ACK = 2;
	public static final int STATE_ESTABLISHED = 3;
	public static final int STATE_FAILED = 4;

	public static final int DATA_SAVING_NEVER=0;
	public static final int DATA_SAVING_MOBILE=1;
	public static final int DATA_SAVING_ALWAYS=2;

	public static final int ERROR_LOCALIZED=-3;
	public static final int ERROR_PRIVACY=-2;
	public static final int ERROR_PEER_OUTDATED=-1;
	public static final int ERROR_UNKNOWN=0;
	public static final int ERROR_INCOMPATIBLE=1;
	public static final int ERROR_TIMEOUT=2;
	public static final int ERROR_AUDIO_IO=3;

	private long nativeInst = 0;
	private long callStartTime;
	private ConnectionStateListener listener;

	public VoIPController() {
		nativeInst = nativeInit(Build.VERSION.SDK_INT);
	}

	public void start() {
		ensureNativeInstance();
		nativeStart(nativeInst);
	}

	public void connect() {
		ensureNativeInstance();
		nativeConnect(nativeInst);
	}

	public void setRemoteEndpoints(TLRPC.TL_phoneConnection[] endpoints, boolean allowP2p) {
		if (endpoints.length == 0) {
			throw new IllegalArgumentException("endpoints size is 0");
		}
		for (int a = 0; a < endpoints.length; a++) {
			TLRPC.TL_phoneConnection endpoint = endpoints[a];
			if (endpoint.ip == null || endpoint.ip.length() == 0) {
				throw new IllegalArgumentException("endpoint " + endpoint + " has empty/null ipv4");
			}
			if (endpoint.peer_tag != null && endpoint.peer_tag.length != 16) {
				throw new IllegalArgumentException("endpoint " + endpoint + " has peer_tag of wrong length");
			}
		}
		ensureNativeInstance();
		nativeSetRemoteEndpoints(nativeInst, endpoints, allowP2p);
	}

	public void setEncryptionKey(byte[] key, boolean isOutgoing) {
		if (key.length != 256) {
			throw new IllegalArgumentException("key length must be exactly 256 bytes but is " + key.length);
		}
		ensureNativeInstance();
		nativeSetEncryptionKey(nativeInst, key, isOutgoing);
	}

	public static void setNativeBufferSize(int size) {
		nativeSetNativeBufferSize(size);
	}

	public void release() {
		ensureNativeInstance();
		nativeRelease(nativeInst);
		nativeInst = 0;
	}

	public String getDebugString() {
		ensureNativeInstance();
		return nativeGetDebugString(nativeInst);
	}

	private void ensureNativeInstance() {
		if (nativeInst == 0) {
			throw new IllegalStateException("Native instance is not valid");
		}
	}

	public void setConnectionStateListener(ConnectionStateListener connectionStateListener) {
		listener = connectionStateListener;
	}

	private void handleStateChange(int state) {
		callStartTime = SystemClock.elapsedRealtime();
		if (listener != null) {
			listener.onConnectionStateChanged(state);
		}
	}

	public void setNetworkType(int type) {
		ensureNativeInstance();
		nativeSetNetworkType(nativeInst, type);
	}

	public long getCallDuration() {
		return SystemClock.elapsedRealtime() - callStartTime;
	}

	public void setMicMute(boolean mute) {
		ensureNativeInstance();
		nativeSetMicMute(nativeInst, mute);
	}

	public void setConfig(double recvTimeout, double initTimeout, int dataSavingOption){
		ensureNativeInstance();
		boolean sysAecAvailable=false, sysNsAvailable=false;
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN){
			try{
				sysAecAvailable=AcousticEchoCanceler.isAvailable();
				sysNsAvailable=AcousticEchoCanceler.isAvailable();
			}catch(Throwable x){

			}
		}
		nativeSetConfig(nativeInst, recvTimeout, initTimeout, dataSavingOption,
				Build.VERSION.SDK_INT<Build.VERSION_CODES.JELLY_BEAN || !(sysAecAvailable && VoIPServerConfig.getBoolean("use_system_aec", true)),
				Build.VERSION.SDK_INT<Build.VERSION_CODES.JELLY_BEAN || !(sysNsAvailable && VoIPServerConfig.getBoolean("use_system_ns", true)),
				true, BuildConfig.DEBUG ? getLogFilePath() : null);
	}

	public void debugCtl(int request, int param){
		ensureNativeInstance();
		nativeDebugCtl(nativeInst, request, param);
	}

	public long getPreferredRelayID(){
		ensureNativeInstance();
		return nativeGetPreferredRelayID(nativeInst);
	}

	public int getLastError(){
		ensureNativeInstance();
		return nativeGetLastError(nativeInst);
	}

	public void getStats(Stats stats){
		ensureNativeInstance();
		if(stats==null)
			throw new NullPointerException("You're not supposed to pass null here");
		nativeGetStats(nativeInst, stats);
	}

	public static String getVersion(){
		return nativeGetVersion();
	}

	private String getLogFilePath(){
		Calendar c=Calendar.getInstance();
		return new File(ApplicationLoader.applicationContext.getExternalFilesDir(null),
				String.format(Locale.US, "logs/%02d_%02d_%04d_%02d_%02d_%02d_voip.txt",
						c.get(Calendar.DATE), c.get(Calendar.MONTH)+1, c.get(Calendar.YEAR),
						c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND))).getAbsolutePath();
	}

	public String getDebugLog(){
		ensureNativeInstance();
		return nativeGetDebugLog(nativeInst);
	}

	private native long nativeInit(int systemVersion);
	private native void nativeStart(long inst);
	private native void nativeConnect(long inst);
	private static native void nativeSetNativeBufferSize(int size);
	private native void nativeSetRemoteEndpoints(long inst, TLRPC.TL_phoneConnection[] endpoints, boolean allowP2p);
	private native void nativeRelease(long inst);
	private native void nativeSetNetworkType(long inst, int type);
	private native void nativeSetMicMute(long inst, boolean mute);
	private native void nativeDebugCtl(long inst, int request, int param);
	private native void nativeGetStats(long inst, Stats stats);
	private native void nativeSetConfig(long inst, double recvTimeout, double initTimeout, int dataSavingOption, boolean enableAEC, boolean enableNS, boolean enableAGC, String logFilePath);
	private native void nativeSetEncryptionKey(long inst, byte[] key, boolean isOutgoing);
	private native long nativeGetPreferredRelayID(long inst);
	private native int nativeGetLastError(long inst);
	private native String nativeGetDebugString(long inst);
	private static native String nativeGetVersion();
	private native String nativeGetDebugLog(long inst);

	public interface ConnectionStateListener {
		void onConnectionStateChanged(int newState);
	}

	public static class Stats{
		public long bytesSentWifi;
		public long bytesRecvdWifi;
		public long bytesSentMobile;
		public long bytesRecvdMobile;

		@Override
		public String toString(){
			return "Stats{"+
					"bytesRecvdMobile="+bytesRecvdMobile+
					", bytesSentWifi="+bytesSentWifi+
					", bytesRecvdWifi="+bytesRecvdWifi+
					", bytesSentMobile="+bytesSentMobile+
					'}';
		}
	}
}
