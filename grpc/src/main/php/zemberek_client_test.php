<?php
require dirname(__FILE__).'/vendor/autoload.php';

@include_once dirname(__FILE__).'/Zemberek/Langid/LanguageIdServiceClient.php';
@include_once dirname(__FILE__).'/Zemberek/Normalization/NormalizationServiceClient.php';
@include_once dirname(__FILE__).'/Zemberek/Simple_analysis/SimpleAnalysisServiceClient.php';
@include_once dirname(__FILE__).'/language_id.pb.php';
@include_once dirname(__FILE__).'/normalization.pb.php';
@include_once dirname(__FILE__).'/simple_analysis.pb.php';

function lang_id($input)
{
    $client = new Zemberek\Langid\LanguageIdServiceClient('localhost:6789', [
        'credentials' => Grpc\ChannelCredentials::createInsecure(),
    ]);
    $request = new Zemberek\Langid\DetectRequest();
    $request->setInput($input);
    list($reply, $status) = $client->Detect($request)->wait();
    $message = $reply->getLangId();

    return $message;
}

function normalize($input)
{
    $client = new Zemberek\Normalization\NormalizationServiceClient('localhost:6789', [
        'credentials' => Grpc\ChannelCredentials::createInsecure(),
    ]);
    $request = new Zemberek\Normalization\NormalizationRequest();
    $request->setInput($input);
    list($reply, $status) = $client->Normalize($request)->wait();
    $message = $reply->getNormalizedInput();

    return $message;
}

function findLemmas($sentence)
{
    $client = new Zemberek\Simple_analysis\SimpleAnalysisServiceClient('localhost:6789', [
        'credentials' => Grpc\ChannelCredentials::createInsecure(),
    ]);
    $request = new Zemberek\Simple_analysis\SentenceRequest();
    $request->setInput($sentence);
    list($reply, $status) = $client->AnalyzeSentence($request)->wait();

    $results = $reply->getResults();

    foreach($results as $result) {
      $best = $result->getBest();
      $token = $result->getToken();
      $lemmas = $best->getLemmas();

      // implode nedense çalışmadı. $lemmas herhalde array değil özel bir nesne.

      $lemmaStr = '';
      foreach($lemmas as $lemma) {
        $lemmaStr = $lemmaStr.' '.$lemma;
      }

      echo $token.' -> '.$lemmaStr."\n";
    }
}

// Dil bulma

$input = 'Merhaba Dünya!'."\n";
echo "Cümle = ".$input;
echo "Dil = ".lang_id($input)."\n";

// Basit normalizasyon (sadece test amaçlı henüz gerçek normalizasyon yok.)

$noisy = "Mrhaba dnya";
echo "Noisy = ".$noisy."\n";
echo "Normalized = ".normalize($noisy)."\n";

// Cümle için analiz ve kök-gövde bulma.

$sentence = "kavanozun kapağını açtır demiştim sana.";
echo "Cümle = ".$sentence."\n";
findLemmas($sentence);

?>
