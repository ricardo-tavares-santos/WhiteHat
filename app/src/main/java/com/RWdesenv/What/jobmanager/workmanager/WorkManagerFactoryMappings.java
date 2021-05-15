package com.RWdesenv.What.jobmanager.workmanager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.RWdesenv.What.jobs.AttachmentDownloadJob;
import com.RWdesenv.What.jobs.AttachmentUploadJob;
import com.RWdesenv.What.jobs.AvatarDownloadJob;
import com.RWdesenv.What.jobs.CleanPreKeysJob;
import com.RWdesenv.What.jobs.CreateSignedPreKeyJob;
import com.RWdesenv.What.jobs.DirectoryRefreshJob;
import com.RWdesenv.What.jobs.FailingJob;
import com.RWdesenv.What.jobs.FcmRefreshJob;
import com.RWdesenv.What.jobs.LocalBackupJob;
import com.RWdesenv.What.jobs.MmsDownloadJob;
import com.RWdesenv.What.jobs.MmsReceiveJob;
import com.RWdesenv.What.jobs.MmsSendJob;
import com.RWdesenv.What.jobs.MultiDeviceBlockedUpdateJob;
import com.RWdesenv.What.jobs.MultiDeviceConfigurationUpdateJob;
import com.RWdesenv.What.jobs.MultiDeviceContactUpdateJob;
import com.RWdesenv.What.jobs.MultiDeviceGroupUpdateJob;
import com.RWdesenv.What.jobs.MultiDeviceProfileKeyUpdateJob;
import com.RWdesenv.What.jobs.MultiDeviceReadUpdateJob;
import com.RWdesenv.What.jobs.MultiDeviceVerifiedUpdateJob;
import com.RWdesenv.What.jobs.PushDecryptMessageJob;
import com.RWdesenv.What.jobs.PushGroupSendJob;
import com.RWdesenv.What.jobs.PushGroupUpdateJob;
import com.RWdesenv.What.jobs.PushMediaSendJob;
import com.RWdesenv.What.jobs.PushNotificationReceiveJob;
import com.RWdesenv.What.jobs.PushTextSendJob;
import com.RWdesenv.What.jobs.RefreshAttributesJob;
import com.RWdesenv.What.jobs.RefreshPreKeysJob;
import com.RWdesenv.What.jobs.RequestGroupInfoJob;
import com.RWdesenv.What.jobs.RetrieveProfileAvatarJob;
import com.RWdesenv.What.jobs.RetrieveProfileJob;
import com.RWdesenv.What.jobs.RotateCertificateJob;
import com.RWdesenv.What.jobs.RotateProfileKeyJob;
import com.RWdesenv.What.jobs.RotateSignedPreKeyJob;
import com.RWdesenv.What.jobs.SendDeliveryReceiptJob;
import com.RWdesenv.What.jobs.SendReadReceiptJob;
import com.RWdesenv.What.jobs.ServiceOutageDetectionJob;
import com.RWdesenv.What.jobs.SmsReceiveJob;
import com.RWdesenv.What.jobs.SmsSendJob;
import com.RWdesenv.What.jobs.SmsSentJob;
import com.RWdesenv.What.jobs.TrimThreadJob;
import com.RWdesenv.What.jobs.TypingSendJob;
import com.RWdesenv.What.jobs.UpdateApkJob;

import java.util.HashMap;
import java.util.Map;

public class WorkManagerFactoryMappings {

  private static final Map<String, String> FACTORY_MAP = new HashMap<String, String>() {{
    put(AttachmentDownloadJob.class.getName(), AttachmentDownloadJob.KEY);
    put(AttachmentUploadJob.class.getName(), AttachmentUploadJob.KEY);
    put(AvatarDownloadJob.class.getName(), AvatarDownloadJob.KEY);
    put(CleanPreKeysJob.class.getName(), CleanPreKeysJob.KEY);
    put(CreateSignedPreKeyJob.class.getName(), CreateSignedPreKeyJob.KEY);
    put(DirectoryRefreshJob.class.getName(), DirectoryRefreshJob.KEY);
    put(FcmRefreshJob.class.getName(), FcmRefreshJob.KEY);
    put(LocalBackupJob.class.getName(), LocalBackupJob.KEY);
    put(MmsDownloadJob.class.getName(), MmsDownloadJob.KEY);
    put(MmsReceiveJob.class.getName(), MmsReceiveJob.KEY);
    put(MmsSendJob.class.getName(), MmsSendJob.KEY);
    put(MultiDeviceBlockedUpdateJob.class.getName(), MultiDeviceBlockedUpdateJob.KEY);
    put(MultiDeviceConfigurationUpdateJob.class.getName(), MultiDeviceConfigurationUpdateJob.KEY);
    put(MultiDeviceContactUpdateJob.class.getName(), MultiDeviceContactUpdateJob.KEY);
    put(MultiDeviceGroupUpdateJob.class.getName(), MultiDeviceGroupUpdateJob.KEY);
    put(MultiDeviceProfileKeyUpdateJob.class.getName(), MultiDeviceProfileKeyUpdateJob.KEY);
    put(MultiDeviceReadUpdateJob.class.getName(), MultiDeviceReadUpdateJob.KEY);
    put(MultiDeviceVerifiedUpdateJob.class.getName(), MultiDeviceVerifiedUpdateJob.KEY);
    put("PushContentReceiveJob", FailingJob.KEY);
    put("PushDecryptJob", PushDecryptMessageJob.KEY);
    put(PushGroupSendJob.class.getName(), PushGroupSendJob.KEY);
    put(PushGroupUpdateJob.class.getName(), PushGroupUpdateJob.KEY);
    put(PushMediaSendJob.class.getName(), PushMediaSendJob.KEY);
    put(PushNotificationReceiveJob.class.getName(), PushNotificationReceiveJob.KEY);
    put(PushTextSendJob.class.getName(), PushTextSendJob.KEY);
    put(RefreshAttributesJob.class.getName(), RefreshAttributesJob.KEY);
    put(RefreshPreKeysJob.class.getName(), RefreshPreKeysJob.KEY);
    put("RefreshUnidentifiedDeliveryAbilityJob", FailingJob.KEY);
    put(RequestGroupInfoJob.class.getName(), RequestGroupInfoJob.KEY);
    put(RetrieveProfileAvatarJob.class.getName(), RetrieveProfileAvatarJob.KEY);
    put(RetrieveProfileJob.class.getName(), RetrieveProfileJob.KEY);
    put(RotateCertificateJob.class.getName(), RotateCertificateJob.KEY);
    put(RotateProfileKeyJob.class.getName(), RotateProfileKeyJob.KEY);
    put(RotateSignedPreKeyJob.class.getName(), RotateSignedPreKeyJob.KEY);
    put(SendDeliveryReceiptJob.class.getName(), SendDeliveryReceiptJob.KEY);
    put(SendReadReceiptJob.class.getName(), SendReadReceiptJob.KEY);
    put(ServiceOutageDetectionJob.class.getName(), ServiceOutageDetectionJob.KEY);
    put(SmsReceiveJob.class.getName(), SmsReceiveJob.KEY);
    put(SmsSendJob.class.getName(), SmsSendJob.KEY);
    put(SmsSentJob.class.getName(), SmsSentJob.KEY);
    put(TrimThreadJob.class.getName(), TrimThreadJob.KEY);
    put(TypingSendJob.class.getName(), TypingSendJob.KEY);
    put(UpdateApkJob.class.getName(), UpdateApkJob.KEY);
  }};

  public static @Nullable String getFactoryKey(@NonNull String workManagerClass) {
    return FACTORY_MAP.get(workManagerClass);
  }
}
